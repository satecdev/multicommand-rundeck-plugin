package es.satec.rundeck.executors;

import java.util.ArrayList;
import java.util.List;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import es.satec.rundeck.exceptions.CommandException;
import es.satec.rundeck.exceptions.SessionException;
import es.satec.rundeck.helper.ComandRunnerSSH;
import es.satec.rundeck.helper.UserSession;

public class NodeStepExecutorSSH {

	static String noHost = "Unable to find host ";

	public NodeExecutorResult executeCommand(ExecutionContext context, String[] commands, INodeEntry node,
			long timeoutOutput, int numRetriesOutput, String regexExpression) {

		NodeExecutorResult result = NodeExecutorResultImpl.createSuccess(node);
		ComandRunnerSSH runner = new ComandRunnerSSH();
		List<UserSession> userSessions = new ArrayList<>();
		String jumpHosts = node.getAttributes().get("ssh-jump-hosts");

		try {
			if (jumpHosts != null) {
				String[] jumpHostsList = jumpHosts.split(",");
				for (String host : jumpHostsList) {
					for (INodeEntry nodep : context.getNodeService().getNodeSet(context.getFrameworkProject())) {
						if (host != null && host.equals(nodep.getNodename())) {
							UserSession tempSession = runner.getSession(context, nodep);
							if (tempSession != null) {
								userSessions.add(tempSession);
							} else {
								result = NodeExecutorResultImpl.createFailure(NodeStepFailureReason.HostNotFound,
										noHost + nodep.getHostname(), node);
								return result;
							}
						}
					}

				}
			}

			UserSession hostSession = runner.getSession(context, node);
			if (hostSession != null) {
				userSessions.add(hostSession);
			} else {
				result = NodeExecutorResultImpl.createFailure(NodeStepFailureReason.ConnectionFailure,
						noHost + node.getHostname(), node);
				return result;
			}
		} catch (Exception e) {
			result = NodeExecutorResultImpl.createFailure(NodeStepFailureReason.HostNotFound, e.getMessage(), node);
			return result;
		}
		try {
			if (userSessions.isEmpty()) {
				result = NodeExecutorResultImpl.createFailure(NodeStepFailureReason.HostNotFound,
						noHost + node.getHostname(), node);
			} else {
				runner.runSessions(userSessions, context, session -> runner.runCommands(session, context, commands,
						timeoutOutput, numRetriesOutput, regexExpression));
			}
		} catch (SessionException e) {
			result = NodeExecutorResultImpl.createFailure(NodeStepFailureReason.ConnectionFailure, e.getMessage(),
					node);
			return result;

		} catch (CommandException e) {
			result = NodeExecutorResultImpl.createFailure(NodeStepFailureReason.NonZeroResultCode, e.getMessage(),
					node);
			return result;

		}
		return result;

	}

}