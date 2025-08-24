package peer_recommendation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public abstract class BaseAgent extends Agent {

	private static final long serialVersionUID = 1L;

	public static final String REQUEST = "REQUEST";
	public static final String ANSWER = "ANSWER";
	public static final String THANKS = "THANKS";
	public static final String START = "START";
	public static final String REGISTERED = "REGISTERED";
	public static final String INFORM = "INFORM";
	public static final String UNEXPECTED_MSG = "RECEIVED AN UNEXPECTED MESSAGE FROM";
	public static final String CREATE = "CREATE";
	public static final String CREATOR = "Creator";

	public static final String CONN_DETAILS = "CONN_DETAILS";
	public static final String ARC_AVAILABLE = "ARC_AVAILABLE";
	public static final String ARC_CONN_REQUEST = "ARC_CONN_REQUEST";
	public static final String ARC_REQUEST = "ARC_REQUEST";
	public static final String ARC_UPDATE = "ARC_UPDATE";
	public static final String ARC_SEND = "ARC_SEND";
	public static final String ARC_INIT = "ARC_INIT";
	public static final String ARC_PART = "ARC_PART";

	public static final Hashtable<String, ArrayList<Integer>> archivesReference = new Hashtable<>(Map.of(
		"Archive_1", new ArrayList<>(Arrays.asList(1)),
		"Archive_2", new ArrayList<>(Arrays.asList(1))
	));
	public Hashtable<String, Map<Integer, ArrayList<AID>>> fileSystemBase;

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\033[1;93m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	protected static final Random rand = new Random();

	protected static final Logger logger = Logger.getLogger(BaseAgent.class.getName());

	protected static final Long TIMEOUT_LIMIT = 1000L;
	protected static final Long REQUEST_TIMEOUT_LIMIT = 2000L;

	private final transient Object lock = new Object();

	@Override
	protected void setup() {
	}

	protected CyclicBehaviour handleMessages() {
		return new CyclicBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				ACLMessage msg = receive();

				if (msg == null)
					block();
				else {
					switch (msg.getPerformative()) {
						case ACLMessage.INFORM:
							addBehaviour(handleInform(msg));
							break;
						case ACLMessage.REQUEST:
							addBehaviour(handleRequest(msg));
							break;
						case ACLMessage.CFP:
						case ACLMessage.PROPOSE:
						case ACLMessage.ACCEPT_PROPOSAL:
						case ACLMessage.REJECT_PROPOSAL:
							addBehaviour(handleCfp(msg));
							break;
						case ACLMessage.REFUSE:
							addBehaviour(handleRefuse(msg));
							break;
						default:
							logger.log(Level.INFO,
									String.format("%s RECEIVED UNEXPECTED MESSAGE PERFORMATIVE FROM %s", getLocalName(),
											msg.getSender().getLocalName()));
					}
				}
			}
		};
	}

	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				/*
				 * TO-DO:
				 * IMPLEMENT THIS METHOD BEHAVIOUR ON CONCRETE CLASS
				 */
				msg.createReply();
			}
		};
	}

	protected OneShotBehaviour handleRequest(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				/*
				 * TO-DO:
				 * IMPLEMENT THIS METHOD BEHAVIOUR ON CONCRETE CLASS
				 */
				msg.createReply();
			}
		};
	}

	protected OneShotBehaviour handleCfp(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				/*
				 * TO-DO:
				 * IMPLEMENT THIS METHOD BEHAVIOUR ON CONCRETE CLASS
				 */
				msg.createReply();
			}
		};
	}

	protected OneShotBehaviour handleRefuse(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				/*
				 * TO-DO:
				 * IMPLEMENT THIS METHOD BEHAVIOUR ON CONCRETE CLASS
				 */
				msg.createReply();
			}
		};
	}

	protected WakerBehaviour timeoutBehaviour(AID requestedAgent, String requestedOperation, long timeout) {
		return new WakerBehaviour (this, timeout) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onWake(){
				/*
				 * TO-DO:
				 * IMPLEMENT THIS METHOD BEHAVIOUR ON CONCRETE CLASS
				 */
				ACLMessage newMessage = new ACLMessage(ACLMessage.SUBSCRIBE);
				newMessage.addReceiver(requestedAgent);
				newMessage.setContent(String.format("%l %s", timeout, requestedOperation));
				send(newMessage);
			}
		};
	}
	
	protected void registerDF(Agent regAgent, String sdName, String sdType) {
		try {
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());

			ServiceDescription sd = new ServiceDescription();
			sd.setType(sdType);
			sd.setName(sdName);

			DFAgentDescription[] found = DFService.search(this, dfd);

			dfd.addServices(sd);

			if (found.length == 0) {
				DFService.register(regAgent, dfd);
			} else {
				found[0].addServices(sd);
				DFService.modify(regAgent, found[0]);
			}

			logger.log(Level.INFO, String.format("%s REGISTERED WITH THE DF", getLocalName()));
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	protected void registerDF(Agent regAgent, ArrayList<String> specs) {
		try {
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());

			for ( String specsInfo : specs ) {
				ServiceDescription sd = new ServiceDescription();
				sd.setType(specsInfo);
				sd.setName(specsInfo);

				DFAgentDescription[] found = DFService.search(this, dfd);

				dfd.addServices(sd);

				if (found.length == 0) {
					DFService.register(regAgent, dfd);
				} else {
					found[0].addServices(sd);
					DFService.modify(regAgent, found[0]);
				}
			}

			logger.log(Level.INFO, String.format("%s REGISTERED WITH THE DF", getLocalName()));
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	protected DFAgentDescription[] searchAgentByType(String type) {
		SearchConstraints sc = new SearchConstraints();
		sc.setMaxResults(-1L);
		DFAgentDescription search = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		DFAgentDescription[] foundAgents = null;

		sd.setType(type);
		search.addServices(sd);

		try {
			foundAgents = DFService.search(this, search, sc);
		} catch (Exception any) {
			any.printStackTrace();
		}

		return foundAgents;
	}

	protected DFAgentDescription[] searchAgentByType(String[] type) {
		SearchConstraints sc = new SearchConstraints();
		sc.setMaxResults(-1L);
		DFAgentDescription search = new DFAgentDescription();

		DFAgentDescription[] foundAgents = null;

		for (int i = 0; i < type.length; ++i) {
			ServiceDescription sd = new ServiceDescription();
			sd.setType(type[i]);
			search.addServices(sd);
		}

		try {
			foundAgents = DFService.search(this, search, sc);
		} catch (Exception any) {
			any.printStackTrace();
		}

		return foundAgents;
	}

	@Override
	protected void takeDown() {
		// Deregister with the DF
		try {
			DFService.deregister(this);
			logger.log(Level.INFO, String.format("%s DEREGISTERED WITH THE DF", getLocalName()));

		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	protected void sendMessage(String agentName, int performative, String content) {
		ACLMessage msg = new ACLMessage(performative);
		msg.setContent(content);
		msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
		send(msg);
	}

	protected void loggerSetup() {
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new LogFormatter());
		logger.setUseParentHandlers(false);
		logger.addHandler(handler);
	}

	protected void resetFileSystemBase () {
		fileSystemBase = new Hashtable<>();

		for ( Map.Entry<String, ArrayList<Integer>> entryParts : archivesReference.entrySet() ) {
			for ( int part : entryParts.getValue() ) {
				fileSystemBase.put(entryParts.getKey(), new Hashtable<>(Map.of(
					part, new ArrayList<>()
				)));
			}
		}
	}

	protected void printAllFiles () {
        synchronized (lock) {
            for(Map.Entry<String, Map<Integer, ArrayList<AID>>> entry : fileSystemBase.entrySet()){
                logger.log(Level.INFO, String.format("%sData for current file: %s %s", ANSI_YELLOW, entry.getKey(), ANSI_RESET));

                for (Map.Entry<Integer, ArrayList<AID>> entryParts : entry.getValue().entrySet()) {
                    StringBuilder strBld = new StringBuilder();
                    strBld.append(String.format("%s - %d: ", ANSI_YELLOW, entryParts.getKey()));
                    for ( AID ag : entryParts.getValue() ) {
                        strBld.append(String.format("%s ", ag.getLocalName()));
                    }
                    strBld.append(ANSI_RESET);

                    logger.log(Level.INFO, strBld.toString());
                }
            }
        }
    }
}

