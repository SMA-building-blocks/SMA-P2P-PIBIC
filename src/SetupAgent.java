
import java.io.IOException;

import jade.core.AID;
import jade.core.Agent;
import jade.core.Runtime;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.tools.DummyAgent.DummyAgent;

public class SetupAgent extends Agent {
	private static final long serialVersionUID = 1L;
	private DFAgentDescription dfd;
	
	private AID[] communicatorAgents;
	
	protected void setup () {
		dfd = new DFAgentDescription();
		dfd.setName(getAID());
		
		ServiceDescription sd = new ServiceDescription();
		sd.setType("setup-agent");
		sd.setName("JADE-book-selling-setup-agent");
		dfd.addServices(sd);	
		
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new SetupEverything());
	}
	
	private class SetupEverything extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		
		public void action () {
			Runtime rt = Runtime.instance();
			
			Profile p = new ProfileImpl();
			
			ContainerController cc = rt.createAgentContainer(p);
			
			Object reference = new Object();
			
			Object arg[] = new Object[1];
			
			arg[0] = reference;
			
			AgentController communicator1 = null;
			
			try {
				communicator1 = cc.createNewAgent("communicator1", "CommunicatorAgent", arg);
				
				communicator1.start();
				
				
			} catch ( StaleProxyException e ) {
				e.printStackTrace();
			}
			
			try {
	            System.out.println("The system is paused -- this action is only here to let you activate the sniffer on the agents, if you want (see documentation)");
	            System.out.println("Press enter in the console to start the agents");
	            System.in.read();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			
			ACLMessage isAlive = new ACLMessage(ACLMessage.REQUEST);
			
//			DFAgentDescription template = new DFAgentDescription();
//			ServiceDescription sd = new ServiceDescription();
//			sd.setType("jade.tools.DummyAgent.DummyAgent");
//			template.addServices(sd);
			// Update the list of seller agents
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd2 = new ServiceDescription();
			sd2.setType("communicator");
			template.addServices(sd2);
			
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				if ( result.length > 0 ) {
					System.out.println("ACHEI!" + result.toString());
				} else {
					System.out.println("Não achei...");
				}
				communicatorAgents = new AID[result.length + 1];
				
				for (int i = 0; i < result.length; ++i) {
					communicatorAgents[i] = result[i].getName();
					System.out.println("ALOU: " + communicatorAgents[i].getName());
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
			
			isAlive.setContent("Are you alive?");
			isAlive.setConversationId("setup-agent");
			
			for ( AID agent : communicatorAgents ) {
				isAlive.addReceiver(agent);
			}
			
			myAgent.send(isAlive);
			
//			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("setup-agent"),
//					MessageTemplate.MatchInReplyTo(isAlive.getReplyWith()));
//			
//			ACLMessage reply = myAgent.receive(mt);
			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
			
			ACLMessage recv_msg = myAgent.receive(mt);
			
			if ( recv_msg != null ) {
				
				String title = recv_msg.getContent();
				
				System.out.println("Mensagem recebida: " + title);
				
			}
		}
		
		
		
	}
	
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("bye bye");
	}
}
