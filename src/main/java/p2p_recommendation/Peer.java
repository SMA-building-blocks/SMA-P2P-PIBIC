package p2p_recommendation;

import java.util.ArrayList;
import java.util.logging.Level;

import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;

public class Peer extends BaseAgent {

	private static final long serialVersionUID = 1L;
	private ArrayList<String> ownedArchives = new ArrayList<>();

	@Override
	protected void setup() {
		addBehaviour(handleMessages());

		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			for ( Object arc : args ) {
				ownedArchives.add(arc.toString());
			}
		}

		String helloMsg =  String.format("I'm %s", this.getLocalName(), 
			( ownedArchives.isEmpty() ? "!" : " and I am a seeder!" ));


		logger.log(Level.INFO, helloMsg);

		this.registerDF(this, "Peer", "peer");
	}

	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				String [] splittedMsg = msg.getContent().split(" ");

				switch (splittedMsg[0]) {
					case START:
						updateOwnedArchivesFS();
						break;
					default:
						logger.log(Level.INFO, 
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG, msg.getSender().getLocalName()));
						break;
				}
			}
		};
	}

	protected void updateOwnedArchivesFS () {
		StringBuilder strBld = new StringBuilder();

		strBld.append(String.format("%s %d", ARC_UPDATE, ownedArchives.size()));

		for ( String arc : ownedArchives ) {
			strBld.append(String.format(" %s 1 1", arc));
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