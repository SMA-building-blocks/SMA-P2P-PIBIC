package p2p_recommendation;

import java.util.ArrayList;
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
						else addBehaviour(requestTimeout(TIMEOUT_LIMIT));
						break;
					case INFORM:
						sendFileRequestProposal(splittedMsg);
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
				/*
				 * TO-DO:
				 * IMPLEMENT THIS METHOD BEHAVIOUR ON CONCRETE CLASS
				 */
				msg.createReply();
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
			}
		}

		/*
		* TO-DO: 
		* Given all seeders by archive, we must require'em some 
		* connection data to select one
		*/
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
		// String.format("%s %s %s %d", ARC_CONN_REQUEST, arcName, ARC_PART, k);

		int connVel = rand.nextInt(1, 11);
		String arcName = splittedMsg[1];
		int arcPart = Integer.parseInt(splittedMsg[3]);

		// CONN_DETAILS 1_a_10 ARC_AVAILABLE 1 arcName ARC_PART k
		if ( filePartAvailable(arcName, arcPart) ) {

		} else {
			sendMessage(sender.getLocalName(), ACLMessage.REFUSE, String.format("%s %d %s %d", CONN_DETAILS, connVel, ARC_AVAILABLE, 0));
		}

		
	}

	private boolean filePartAvailable (String arcName, int arcPart) {
		/*
		 * TO-DO: 
		 * verify if the requested part is available at a local fileBase
		 */
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

			if ( !missingParts.isEmpty() ) requestArchive(entryParts.getKey());
		}
		
		addBehaviour(requestTimeout(REQUEST_TIMEOUT_LIMIT));
	}

	private void requestArchive ( String arcName ) {
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