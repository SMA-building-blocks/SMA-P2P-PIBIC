package p2p_recommendation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;

public class Peer extends BaseAgent {

	private static final long serialVersionUID = 1L;
	private Hashtable<String, ArrayList<Integer>> ownedArchives = new Hashtable<>();
	private static Map<String, ArrayList<AID>>  connRequested = Collections.synchronizedMap(new HashMap<>());
	private static Map<String, Pair>  connInfos = Collections.synchronizedMap(new HashMap<>());
	
	private boolean amIASeeder = false;

	@Override
	protected void setup() {
		addBehaviour(handleMessages());

		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			for ( Object arc : args ) {
				ownedArchives.put(arc.toString(), new ArrayList<>(archivesReference.get(arc.toString())));
			}
		}

		resetFileSystemBase();

		if ( !ownedArchives.isEmpty() ) amIASeeder = true;

		String helloMsg =  String.format("I'm %s", this.getLocalName(), 
			( !amIASeeder ? "!" : " and I am a seeder!" ));


		logger.log(Level.INFO, helloMsg);

		this.registerDF(this, "Peer", "Peer");
	}

	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				String [] splittedMsg = msg.getContent().split(" ");

				switch (splittedMsg[0]) {
					case START:
						if ( amIASeeder ) updateOwnedArchivesFS();
						addBehaviour(requestTimeout(TIMEOUT_LIMIT));
						break;
					case INFORM:
						sendFileRequestProposal(splittedMsg);
						break;
					case ARC_SEND:
						handleRecvArc(splittedMsg);
						break;
					default:
						logger.log(Level.INFO, 
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG, msg.getSender().getLocalName()));
						break;
				}
			}
		};
	}

	protected OneShotBehaviour handleCfp(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				String [] splittedMsg = msg.getContent().split(" ");

				switch (splittedMsg[0]) {
					case ARC_CONN_REQUEST:
						handleConnectionRequest(msg.getSender(), splittedMsg);
						break;
					case CONN_DETAILS:
						handleConnectionDetails(msg.getSender(), splittedMsg);
						break;
					case ARC_REQUEST:
						handleArcRequest(msg);
						break;
					default:
						logger.log(Level.INFO, 
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG, msg.getSender().getLocalName()));
						break;
				}
			}
		};
	}

	protected OneShotBehaviour handleRefuse(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				String [] splittedMsg = msg.getContent().split(" ");

				switch (splittedMsg[0]) {
					case CONN_DETAILS:
						logger.log(Level.INFO, 
							String.format("%s CFP Refused by %s %s", ANSI_YELLOW, msg.getSender().getLocalName(), ANSI_RESET));
						break;
					default:
						logger.log(Level.INFO, 
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG, msg.getSender().getLocalName()));
						break;
				}
			}
		};
	}


	private WakerBehaviour requestTimeout(long timeout) {
		return new WakerBehaviour(this, timeout) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onWake() {
				if ( !doIHaveAllArchives() ) requestAllArchives();
			}
		};
	}

	private void sendFileRequestProposal ( String [] msgContent ) {
		String arcName = msgContent[1];
		int qtdParts = Integer.parseInt(msgContent[2]);
		parseRecvFileData(msgContent, arcName, qtdParts);

		Map<Integer, ArrayList<AID>> seedersByArchive = fileSystemBase.get(arcName);

		for ( int k : seedersByArchive.keySet() ) {
			String msgCnt = String.format("%s %s %s %d", ARC_CONN_REQUEST, arcName, ARC_PART, k);
			for ( AID seeder : seedersByArchive.get(k) ) {
				sendMessage(seeder.getLocalName(), ACLMessage.CFP, msgCnt);
				if ( connRequested.get(hashArcPart(arcName, Integer.toString(k))) == null ) {
					connRequested.put(hashArcPart(arcName, Integer.toString(k)), new ArrayList<>(Arrays.asList(new AID(seeder.getLocalName(), AID.ISLOCALNAME))));
				} else {
					connRequested.put(hashArcPart(arcName, Integer.toString(k)), connRequested.get(hashArcPart(arcName, Integer.toString(k))) );
				}
			}
		}
	}

	private void parseRecvFileData(String[] msgContent, String arcName, int qtdParts) {
		HashMap<Integer, ArrayList<AID>> seedersByPart = new HashMap<>();

		int cntParts = 0, idx = 3;
		while ( cntParts < qtdParts ) {
			int currentPart = Integer.parseInt(msgContent[idx++]);
			int numAgents = Integer.parseInt(msgContent[idx]);

			if ( numAgents == 0 ) {
				++cntParts;
				++idx;
				continue;
			}

			ArrayList<AID> seeders = new ArrayList<>();
			for ( int i = 1; i <= qtdParts; ++i ) {
				seeders.add(new AID(msgContent[idx + i], AID.ISLOCALNAME));
			}

			seedersByPart.put(currentPart, seeders);

			++cntParts;
			idx += numAgents + 1;
		}

		fileSystemBase.put(arcName, seedersByPart);
	}

	private void handleConnectionRequest ( AID sender, String [] splittedMsg ) {
		int connVel = rand.nextInt(1, 11);
		String arcName = splittedMsg[1];
		int arcPart = Integer.parseInt(splittedMsg[3]);
		boolean filePartAvb =  filePartAvailable(arcName, arcPart);

		// CONN_DETAILS 1_a_10 ARC_AVAILABLE 1 arcName ARC_PART k
		String messageContent;
		
		if ( filePartAvb ) {
			messageContent = String.format("%s %d %s %d %s %s %s", CONN_DETAILS, connVel, ARC_AVAILABLE, 1, arcName, ARC_PART, arcPart);
		} else {
			messageContent = String.format("%s %d %s %d", CONN_DETAILS, connVel, ARC_AVAILABLE, 0);
		}
		
		sendMessage(sender.getLocalName(), filePartAvb? ACLMessage.PROPOSE: ACLMessage.REFUSE, messageContent);
	}

	private void handleConnectionDetails ( AID sender, String [] splittedMsg ) {
		if ( Integer.parseInt(splittedMsg[3]) == 1 ) {
			Pair connSpeed = new Pair(new AID(sender.getLocalName(), AID.ISLOCALNAME), Integer.parseInt(splittedMsg[1]));

			if ( connInfos.get(hashArcPart(splittedMsg[4], splittedMsg[6])) == null ) {
				connInfos.put(hashArcPart(splittedMsg[4], splittedMsg[6]), connSpeed);
			} else {
				if ( connInfos.get(hashArcPart(splittedMsg[4], splittedMsg[6])).value < connSpeed.value ) {
					connInfos.put(hashArcPart(splittedMsg[4], splittedMsg[6]), connSpeed);
				}
			}

			connRequested.remove(hashArcPart(splittedMsg[4], splittedMsg[6]));

			if ( connRequested.isEmpty() ) {
				requestArchive(connInfos.get(hashArcPart(splittedMsg[4], splittedMsg[6])), splittedMsg[4], splittedMsg[6]);
			}
		}
	}

	private String hashArcPart ( String arcName, String arcPart ) {
		return arcName + '-' + arcPart;
	}

	private void requestArchive ( Pair pair, String arcName, String arcPart ) {
		sendMessage(pair.key.getLocalName(), ACLMessage.ACCEPT_PROPOSAL, String.format("%s %s %s %s", ARC_REQUEST, arcName, ARC_PART, arcPart));
	}

	private void handleArcRequest ( ACLMessage message ) {
		String [] splittedMsg = message.getContent().split(" ");
		String arcName = splittedMsg[1];
		String arcPart = splittedMsg[3]; 

		ACLMessage newMsg = message.createReply();
		newMsg.setContent(String.format("%s %s %s %s", ARC_SEND, arcName, ARC_PART, arcPart));
		newMsg.setPerformative(ACLMessage.INFORM);
		send(newMsg); 
	}

	private void handleRecvArc ( String [] splittedMsg ) {
		String arcName = splittedMsg[1];
		int arcPart = Integer.parseInt(splittedMsg[3]);

		if ( ownedArchives.get(arcName) == null ) {
			ownedArchives.put(arcName, new ArrayList<>(Arrays.asList(arcPart)));
		} else {
			ArrayList<Integer> ownedParts = ownedArchives.get(arcName);
			ownedParts.add(arcPart);
			ownedArchives.put(arcName, ownedParts);
		}

		String logMsg = String.format("%s I'm %s and I've just received %s (part %d)! %s", ANSI_GREEN, this.getLocalName(), arcName, arcPart, ANSI_RESET);
		logger.log(Level.INFO, logMsg);

		updateOwnedArchivesFS();

		if ( doIHaveAllArchives() ) {
			logMsg = String.format("%s I'm %s and I own all archives! %s", ANSI_GREEN, this.getLocalName(), ANSI_RESET);
			logger.log(Level.INFO, logMsg);
		}
	}

	private boolean filePartAvailable (String arcName, int arcPart) {
		if ( fileSystemBase.get(arcName) == null ) {
			return false;
		}
		if ( fileSystemBase.get(arcName).get(arcPart) == null ) {
			return false;
		}
		return true;
	}

	private void requestAllArchives () {
		for ( Map.Entry<String, ArrayList<Integer>> entryParts : archivesReference.entrySet() ) {
			ArrayList<Integer> allOwnedParts = ownedArchives.get(entryParts.getKey());
			Set<Integer> missingParts = new HashSet<>(entryParts.getValue());
			
			if ( allOwnedParts != null ) {
				Set<Integer> ownedParts = new HashSet<>(allOwnedParts);
				missingParts.removeAll(ownedParts);
			}

			if ( !missingParts.isEmpty() ) requestArchiveList(entryParts.getKey());
		}
		
		addBehaviour(requestTimeout(REQUEST_TIMEOUT_LIMIT));
	}

	private void requestArchiveList ( String arcName ) {
		DFAgentDescription[] fsAgent = searchAgentByType("FileServer");

		try {
			sendMessage(fsAgent[0].getName().getLocalName(), ACLMessage.REQUEST, String.format("%s %s", REQUEST, arcName));
		} catch (Exception e) {
			logger.log(Level.WARNING, String.format("Agent FileServer Not Found: %s", e.toString()));
		}
	}

	private boolean doIHaveAllArchives () {
		for ( Map.Entry<String, ArrayList<Integer>> entryParts : archivesReference.entrySet() ) {
			ArrayList<Integer> allOwnedParts = ownedArchives.get(entryParts.getKey());
			
			if ( allOwnedParts == null ) return false;

			Set<Integer> ownedParts = new HashSet<>(allOwnedParts);
			Set<Integer> referenceParts = new HashSet<>(entryParts.getValue());

			if ( !ownedParts.equals(referenceParts) ) return false;
		}
	
		return true;
	}

	private void updateOwnedArchivesFS () {
		StringBuilder strBld = new StringBuilder();

		strBld.append(String.format("%s %d", ARC_UPDATE, ownedArchives.size()));

		for ( Map.Entry<String, ArrayList<Integer>> entryParts : ownedArchives.entrySet() ) {
			strBld.append(String.format(" %s %d", entryParts.getKey(), entryParts.getValue().size()));
			for ( int part : entryParts.getValue() ) 
				strBld.append(String.format(" %d", part));
		}

		DFAgentDescription[] fsAgent = searchAgentByType("FileServer");

		try {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent(strBld.toString());
			msg.addReceiver(fsAgent[0].getName());
			send(msg);
		} catch (Exception e) {
			logger.log(Level.WARNING, String.format("Agent FileServer Not Found: %s", e.toString()));
		}
	}
}