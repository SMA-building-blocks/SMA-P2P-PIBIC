package peer_recommendation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;


public class Creator extends BaseAgent {

	private static final long serialVersionUID = 1L;
	int peersQuorum = 3;
	ArrayList<String> peersName = new ArrayList<>();

	@Override
	protected void setup() {

		loggerSetup();

		registerDF(this, CREATOR, CREATOR);
		addBehaviour(handleMessages());

		logger.log(Level.INFO, "Starting Agents...");

		logger.log(Level.INFO, "Creating Peers...");

		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			peersQuorum = Math.max(Integer.parseInt(args[0].toString()), peersQuorum);
		}

		ArrayList<String> arcRefNames = new ArrayList<>(archivesReference.keySet());

		try {
			AgentContainer container = getContainerController();
			for (int i = 0; i < peersQuorum; ++i) {
				String peer = "peer_" + i;
				peersName.add(peer);
				Object[] arcRefs = null;

				if ( i < arcRefNames.size() )
					arcRefs = new Object[]{arcRefNames.get(i)};

				this.launchAgent(peer, "peer_recommendation.Peer", arcRefs);
				logger.log(Level.INFO, String.format("%s CREATED AND STARTED NEW PEER: %s ON CONTAINER %s",
						getLocalName(), peer, container.getName()));
			}
		} catch (Exception any) {
			logger.log(Level.SEVERE, String.format("%s ERROR WHILE CREATING AGENTS %s", ANSI_RED, ANSI_RESET));
			any.printStackTrace();
		}

		String fsAgentName = "FileServer";
		launchAgent(fsAgentName, "peer_recommendation.FileServer", null);

		logger.log(Level.INFO, "Agents started...");
		pauseSystem();
		// send them a message demanding start
		logger.log(Level.INFO, "Starting system!");
		
		String content = START;

		for ( String peer : peersName ) {
			sendMessage(peer, ACLMessage.INFORM, content);
			logger.log(Level.INFO, String.format("%s SENT START MESSAGE TO %s", getLocalName(), peer));
		}
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