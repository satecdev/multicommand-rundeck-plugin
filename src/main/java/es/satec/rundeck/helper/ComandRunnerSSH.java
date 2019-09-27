package es.satec.rundeck.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rundeck.storage.api.Resource;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import es.satec.rundeck.exceptions.CommandException;
import es.satec.rundeck.exceptions.SessionException;

public class ComandRunnerSSH {
	@FunctionalInterface
	public interface StablishTunnel {
		public void runCommands(Session session) throws CommandException;
	}

	private Pattern bracketsPattern = Pattern.compile("(\\s*)(\\[)(\\s*)(.*)(\\s*)(\\])(\\s*)");
	private Pattern controlPattern = Pattern.compile("(\\s*)(CTRL|ctrl)(\\s*)(\\-|\\+)(\\s*)([a-zA-Z])(\\s*)");

	public void runSessions(List<UserSession> userSessions, ExecutionContext context, StablishTunnel executor)
			throws SessionException, CommandException {

		if (userSessions.isEmpty()) {
			throw new SessionException("This program expects more than zero session");
		}

		Session session = null;
		Session[] sessions = new Session[userSessions.size()];

		try {

			session = connectTo(userSessions, context, 0, false);

			if (userSessions.size() > 1) {
				for (int i = 1; i < userSessions.size(); i++) {
					session = connectTo(userSessions, context, i, true);
				}
			}
			context.getExecutionLogger().log(5, String.format("The session has been established to %s @ %s",
					session.getUserName(), session.getHost()));
			executor.runCommands(session);

		} finally {
			for (int i = sessions.length - 1; i >= 0; i--) {
				context.getExecutionLogger().log(5, "Disconnecting session");
				if (sessions[i] != null) {
					sessions[i].disconnect();
				}
			}
		}
	}

	public Session connectTo(List<UserSession> userSessions, ExecutionContext context, int i, boolean hasToForward)
			throws SessionException {
		JSch jsch = new JSch();

		if (userSessions.isEmpty()) {
			throw new SessionException("This program expects more than zero session");
		}

		Session session = null;
		Session[] sessions = new Session[userSessions.size()];
		String host = null;
		String user = null;
		String password = null;
		String timeOutCause = null;
		String authCause = null;

		int port = 22;

		Properties config = new Properties();

		try {
			timeOutCause = null;
			authCause = null;
			host = userSessions.get(i).getHostname();
			user = userSessions.get(i).getUsername();
			password = userSessions.get(i).getPassword();
			port = userSessions.get(i).getPort();

			config.put("StrictHostKeyChecking", "no");
			config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
			if (user == null || host == null) {
				throw new JSchException();

			}

			sessions[i] = session = jsch.getSession(user, host, port);
			if (hasToForward) {
				port = session.setPortForwardingL(0, host, port);
			}
			session.setPassword(password);
			session.setConfig(config);
			session.connect(60000);
			context.getExecutionLogger().log(5,
					String.format("Connected to host %s with user %s", host, user));

		} catch (JSchException e) {

			context.getExecutionLogger().log(5, e.getMessage());

			if (e.getMessage().toLowerCase().contains("timed out") || e.getMessage().toLowerCase().contains("timeout")
					|| e.getMessage().toLowerCase().contains("time out")) {
				timeOutCause = "Timeout connecting to host " + host;
			} else if (e.getMessage().toLowerCase().contains("auth fail")) {
				authCause = "Authentication failed to host " + host;
			}

			if (timeOutCause != null) {
				throw new SessionException(timeOutCause);
			} else if (authCause != null) {
				throw new SessionException(authCause);
			} else {
				throw new SessionException(
						String.format("Unable to connect to host %s with user %s. Exception: %s", host,
								user, e.getMessage()));
			}

		}
		return session;
	}

	public void runSessions(List<UserSession> userSessions, PluginStepContext context, StablishTunnel executor)
			throws SessionException, CommandException {
		runSessions(userSessions, context.getExecutionContext(), executor);

	}

	public void runCommands(Session session, ExecutionContext context, String[] commands, long timeoutOutput,
			int numRetriesOutput, String regexExpression) throws CommandException {

		StringBuilder commandsOutput = new StringBuilder();
		Channel channel = null;
		boolean matches = false;

		if (session != null) {
			try {
				channel = session.openChannel("shell");
				channel.setInputStream(null);
				channel.connect();
				context.getExecutionLogger().log(5, "Channel connected.Sending commands: ");
				boolean executionMode = false;

				for (int i = 0; i < commands.length; i++) {
					if (commands[i] != null && !commands[i].isEmpty() && commands[i].trim().length() > 0) {
						boolean endOfScript = (i + 1 == commands.length);
						if ("[".equals(commands[i].trim()) && !executionMode) {
							context.getExecutionLogger().log(5, "Excluding output.. ");
							executionMode = true;
						} else if ((executionMode)) {
							if ("]".equals(commands[i].trim())) {
								context.getExecutionLogger().log(5, "Recovering output.. ");
								commandsOutput
										.append(getOutput(channel, timeoutOutput, numRetriesOutput, true, endOfScript));
								executionMode = false;
							} else {
								context.getExecutionLogger().log(5, "No output.. ");
								commandsOutput.append(sendCommand(commands[i], channel, context, timeoutOutput, 0,
										false, endOfScript));
							}
						} else {
							context.getExecutionLogger().log(5, "With output.. ");
							commandsOutput.append(sendCommand(commands[i], channel, context, timeoutOutput,
									numRetriesOutput, true, endOfScript));
						}
					}
				}

			} catch (Exception e) {
				throw new CommandException(e.getMessage());
			} finally {
				if (channel != null) {
					channel.disconnect();
				}

				String[] splittedCommandList = commandsOutput.toString().replace("\r", "").split("\n");

				for (String splittedCommand : splittedCommandList) {
					context.getExecutionLogger().log(2, splittedCommand);
				}

				if (regexExpression != null) {
					Pattern pattern = Pattern.compile(regexExpression, Pattern.DOTALL | Pattern.MULTILINE);
					Matcher matcher = pattern.matcher(commandsOutput.toString());

					if (matcher.find()) {
						matches = true;
					}

				}
			}

			if (regexExpression != null && !matches) {
				throw new CommandException("Regex expression doesn't match");
			}

		}
	}

	public void runCommands(Session session, PluginStepContext context, String[] commands, long timeoutOutput,
			int numRetriesOutputs, String regexExpression) throws CommandException {
		runCommands(session, context.getExecutionContext(), commands, timeoutOutput, numRetriesOutputs,
				regexExpression);
	}

	private byte[] processCommands(ExecutionContext context, String command) {
		byte[] out = null;
		String newCommand = null;
		if (command != null && !command.isEmpty()) {
			Matcher bracketsMatcher = bracketsPattern.matcher(command);
			if (bracketsMatcher.matches()) {
				Matcher controlMatcher = controlPattern.matcher(bracketsMatcher.group(4));
				if (controlMatcher.matches()) {
					if (controlMatcher.group(6).toLowerCase().trim().length() == 1) {
						char inputChar = controlMatcher.group(6).toLowerCase().charAt(0);
						int asciiValue = (int) inputChar;
						Integer position = (asciiValue - 96);
						context.getExecutionLogger().log(5, position.toString());
						out = new byte[] { position.byteValue() };
					}
				} else {
					newCommand = bracketsMatcher.group(4) + "\r";
					context.getExecutionLogger().log(5, newCommand);
					out = newCommand.getBytes(StandardCharsets.UTF_8);
				}
			} else {
				newCommand = command + "\r";
				context.getExecutionLogger().log(5, newCommand);
				out = newCommand.getBytes(StandardCharsets.UTF_8);
			}
		}
		return out;
	}

	public String sendCommand(String command, Channel channel, ExecutionContext context, long timeoutOutput,
			int maxRetries, boolean hasToWait, boolean endOfScript) throws IOException, InterruptedException {

		OutputStream out = channel.getOutputStream();

		StringBuilder commandsOutput = new StringBuilder();

		String preCommandOutput = getOutput(channel, timeoutOutput, maxRetries, hasToWait, endOfScript);
		if (preCommandOutput != null && !preCommandOutput.isEmpty()) {
			commandsOutput.append(preCommandOutput);
		}

		byte[] result = processCommands(context, command);
		if (result != null) {
			out.write(result);
			out.flush();
		}

		String postCommandOutput = getOutput(channel, timeoutOutput, maxRetries, hasToWait, endOfScript);
		if (postCommandOutput != null && !postCommandOutput.isEmpty()) {
			commandsOutput.append(postCommandOutput);
		}

		return commandsOutput.toString();

	}

	public String getOutput(Channel channel, long timeoutOutput, int maxRetries, boolean hasToWait, boolean endOfScript)
			throws IOException, InterruptedException {
		InputStream in = channel.getInputStream();

		int retries = 0;
		byte[] tmp = new byte[1024 * 64];
		StringBuilder commandsOutput = new StringBuilder();

		if (hasToWait) {
			do {
				while (in.available() > 0) {
					retries = 0;
					int i = in.read(tmp, 0, tmp.length);
					if (i < 0) {
						break;
					}
					String comOut = new String(tmp, 0, i);
					if (comOut != null) {
						commandsOutput.append(comOut);
					}
				}

				if (channel.isClosed()) {

					if (in.available() <= 0) {
						break;
					}
				} else {
					if (commandsOutput.length() > 0 && !endOfScript) {
						break;
					}
					Thread.sleep(timeoutOutput);

				}
			} while (++retries < maxRetries);

			if (retries == maxRetries && commandsOutput.length() <= 0) {
				return null;
			}

		}
		return commandsOutput.toString();
	}

	public UserSession getSession(ExecutionContext context, INodeEntry node) throws IOException, CommandException {
		int port = 22;
		String nodename = null;
		String hostname = null;
		String user = null;
		String password = null;
		UserSession session = null;
		if (node.getHostname() != null) {

			if (node.getHostname().contains(":")) {
				hostname = node.getHostname().split(":")[0];
				port = Integer.valueOf(node.getHostname().split(":")[1]);
			} else {
				hostname = node.getHostname();
			}

		}

		if (node.getNodename() != null) {
			nodename = node.getNodename();
		}

		if (node.getUsername() != null) {
			user = node.getUsername();
		}

		try {
			if (context.getStorageTree() != null && node.getAttributes() != null) {
				Resource<ResourceMeta> passwordPath = context.getStorageTree()
						.getResource(node.getAttributes().get("ssh-password-storage-path"));
				if (passwordPath != null && passwordPath.getContents() != null) {
					InputStream passwordStream = passwordPath.getContents().getInputStream();
					if (passwordStream != null) {
						password = getPassword(passwordStream);
					}
				}

			}
			session = new UserSession(user, password, hostname, nodename, port);

		} catch (Exception e) {
			throw new CommandException(String.format("The key for node  %s and host %s doesnt exist in the keystore",
					node.getNodename(), node.getHostname()));
		}

		return session;
	}

	public UserSession getSession(PluginStepContext context, INodeEntry node) throws IOException, CommandException {
		return getSession(context.getExecutionContext(), node);
	}

	public String getPassword(InputStream passwordStream) throws IOException {
		String password = null;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte[] data = new byte[1024];
		while ((nRead = passwordStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		buffer.flush();
		byte[] byteArray = buffer.toByteArray();

		password = new String(byteArray, StandardCharsets.UTF_8);
		passwordStream.close();
		return password;
	}

}
