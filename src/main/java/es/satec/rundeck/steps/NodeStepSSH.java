package es.satec.rundeck.steps;

import java.util.Map;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;

import es.satec.rundeck.executors.NodeStepExecutorSSH;

@Plugin(name = "ssh-node-step-executor", service = ServiceNameConstants.WorkflowNodeStep)
@PluginDescription(title = "Multicommand SSH Node Step", description = "Define a list of commands using single SSH session")
public class NodeStepSSH implements NodeStepPlugin {

	@PluginProperty(title = "Commands", description = "List of commands to execute", required = true)
	@RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = "CODE")
	protected String commandsList;

	@PluginProperty(title = "Timeout for output", description = "Timeout for output(ms)", required = true, defaultValue = "600")
	protected long timeoutOutput;

	@PluginProperty(title = "Retries for output", description = "Retries for output", required = true, defaultValue = "10")
	protected int numRetriesOutput;

	@PluginProperty(title = "Regex", description = "Regex expression validator", required = false)
	protected String regexExpression;

	public void executeNodeStep(PluginStepContext context, Map<String, Object> configuration, INodeEntry node)
			throws NodeStepException {

		String[] commands = commandsList.split("\\r?\\n");

		NodeStepExecutorSSH ex = new NodeStepExecutorSSH();

		NodeExecutorResult result = ex.executeCommand(context.getExecutionContext(), commands, node, timeoutOutput,
				numRetriesOutput, regexExpression);
		if (!result.isSuccess()) {
			throw new NodeStepException(result.getFailureMessage(), result.getFailureReason(),
					result.getNode().getNodename());
		}

	}
}
