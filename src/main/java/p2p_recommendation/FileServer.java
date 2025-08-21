package p2p_recommendation;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class FileServer extends BaseAgent {

	private static final long serialVersionUID = 1L;
	private final transient Object lock = new Object();

	private Hashtable<String, Map<Integer, ArrayList<AID>>> fileSystemBase;

	@Override
	protected void setup() {
		logger.log(Level.INFO, "Starting the FileServer...");

		resetFileSystemBase();

		logger.log(Level.INFO, String.format("I'm the %s", this.getLocalName()));

		this.registerDF(this, "FileServer", "FileServer");

		addBehaviour(handleMessages());
	}

	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				String [] splittedMsg = msg.getContent().split(" ");

				switch (splittedMsg[0]) {
					case ARC_UPDATE:
						updateArchivesReference(msg.getSender(), splittedMsg);
						break;
					default:
						logger.log(Level.INFO, 
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG, msg.getSender().getLocalName()));
						break;
				}
			}
		};
	}

	private void resetFileSystemBase () {
		fileSystemBase = new Hashtable<>();

		for ( String arc : archivesReference ) {
			fileSystemBase.put(arc, new Hashtable<>());
		}
	}

	private void updateArchivesReference (AID msgSender, String [] msgContent) {
		int arcQtd = Integer.parseInt(msgContent[1]);

		synchronized (lock) {
			int i = 2, arcCnt = 0;
			while ( arcCnt < arcQtd ) {
				if ( !fileSystemBase.containsKey(msgContent[i]) ) 
					fileSystemBase.put(msgContent[i], new Hashtable<>());

				int filePos = i++;
				int partsQtd = Integer.parseInt(msgContent[i]);

				while ( ++i <= (filePos + 1 + partsQtd) ) {
					Map<Integer, ArrayList<AID>> updateMap = fileSystemBase.get(msgContent[filePos]);

					if ( updateMap == null ) updateMap = new Hashtable<Integer, ArrayList<AID>>();

					int currentPart = Integer.parseInt(msgContent[i]);

					ArrayList<AID> fileOwners = updateMap.get(currentPart);

					if ( fileOwners == null ) fileOwners = new ArrayList<>();

					fileOwners.add(msgSender);

					updateMap.put(currentPart, fileOwners);

					fileSystemBase.put(msgContent[filePos], updateMap);
				}

				arcCnt++;
			}
		}
	}
}