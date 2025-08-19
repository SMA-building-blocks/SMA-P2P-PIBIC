package p2p_recommendation;

import java.util.logging.Level;

import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class FileServer extends BaseAgent {

	private static final long serialVersionUID = 1L;

	@Override
	protected void setup() {

		logger.log(Level.INFO, "Starting the FileServer...");

		logger.log(Level.INFO, String.format("I'm the %s", this.getLocalName()));

		this.registerDF(this, "FileServer", "fileServer");

		addBehaviour(handleMessages());
	}

	protected OneShotBehaviour handleInform(ACLMessage msg) {
		return new OneShotBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public void action() {
				msg.createReply();
			}
		};
	}
}