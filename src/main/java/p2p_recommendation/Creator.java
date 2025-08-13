package p2p_recommendation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Class that set the main agent and it's actions
 */
public class Creator extends BaseAgent {

	private static final long serialVersionUID = 1L;
	int workersQuorum = 0;

	@Override
	protected void setup() {

		loggerSetup();

		registerDF(this, CREATOR, CREATOR);
		addBehaviour(handleMessages());

		logger.log(Level.INFO, "Starting Agents...");

		logger.log(Level.INFO, "Creating Workers...");

		ArrayList<String> workersName = new ArrayList<>();

		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			workersQuorum = Integer.parseInt(args[0].toString());
		}

		for (int i = 0; i < workersQuorum; ++i) workersName.add("peer_" + i);

		try {
			AgentContainer container = getContainerController();

			workersName.forEach(worker -> {
				// this.launchAgent(worker, "p2p_recommendation.Peer", null);
				logger.log(Level.INFO, String.format("%s CREATED AND STARTED NEW PEER: %s ON CONTAINER %s",
						getLocalName(), worker, container.getName()));
			});
		} catch (Exception any) {
			logger.log(Level.SEVERE, String.format("%s ERROR WHILE CREATING AGENTS %s", ANSI_RED, ANSI_RESET));
			any.printStackTrace();
		}

		String m1AgentName = "FileServer";
		launchAgent(m1AgentName, "p2p_recommendation.FileServer", null);

		logger.log(Level.INFO, "Agents started...");
		pauseSystem();
		// send them a message demanding start
		logger.log(Level.INFO, "Starting system!");
		
		String content = String.format("START");

		sendMessage(m1AgentName, ACLMessage.INFORM, content);
		logger.log(Level.INFO, String.format("%s SENT START MESSAGE TO %s", getLocalName(), m1AgentName));
	}

	private void pauseSystem() {
		try {
			logger.log(Level.WARNING, String.format(
					"%s The system is paused -- this action is here only to let you activate the sniffer on the agents, if you want (see documentation) %s",
					ANSI_YELLOW, ANSI_RESET));
			logger.log(Level.WARNING,
					String.format("%s Press enter in the console to start the agents %s", ANSI_YELLOW, ANSI_RESET));
			System.in.read();
		} catch (IOException e) {
			logger.log(Level.SEVERE, String.format("%s ERROR STARTING THE SYSTEM %s", ANSI_RED, ANSI_RESET));
			e.printStackTrace();
		}
	}

	private void launchAgent(String agentName, String className, Object[] args) {
		try {
			AgentContainer container = getContainerController(); // get a container controller for creating new agents
			AgentController newAgent = container.createNewAgent(agentName, className, args);
			newAgent.start();
		} catch (Exception e) {
			logger.log(Level.SEVERE, String.format("%s ERROR WHILE LAUNCHING AGENTS %s", ANSI_RED, ANSI_RESET));
			e.printStackTrace();
		}
	}
}