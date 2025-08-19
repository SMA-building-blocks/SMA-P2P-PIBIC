package p2p_recommendation;

import java.util.logging.Level;

import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class Peer extends BaseAgent {

	private static final long serialVersionUID = 1L;

	@Override
	protected void setup() {
		addBehaviour(handleMessages());

		logger.log(Level.INFO, String.format("I'm %s", this.getLocalName()));

		this.registerDF(this, "Peer", "peer");
	}

	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				String [] splittedMsg = msg.getContent().split(" ");

				switch (splittedMsg[0]) {
					case START:
						logger.log(Level.INFO, "I'm a peer and I've just received a start message!");
						break;
					default:
						logger.log(Level.INFO, 
							String.format("%s %s %s", getLocalName(), UNEXPECTED_MSG, msg.getSender().getLocalName()));
						break;
				}
			}
		};
	}

}