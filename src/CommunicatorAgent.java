
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class CommunicatorAgent extends Agent {
	private static final long serialVersionUID = 1L;
	
	protected void setup () {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("communicator");
		sd.setName("p2p-communicator");
		dfd.addServices(sd);	
		System.out.println("Mensagem recebida: " + this.getName() + " socorram-me "+ this.getLocalName());

		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new ReceiveMessage());
		
	}
	
	private class ReceiveMessage extends CyclicBehaviour {
		
		private static final long serialVersionUID = 1L;
		
		public void action () {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			
			ACLMessage msg = myAgent.receive(mt);
			
			if ( msg != null ) {
				
				String title = msg.getContent();
				
				System.out.println("Mensagem recebida: " + title);
				
				ACLMessage reply = msg.createReply();
				
				reply.setPerformative(ACLMessage.CONFIRM);
				
				myAgent.send(reply);
				
			}
			
		}
		
	}
}
