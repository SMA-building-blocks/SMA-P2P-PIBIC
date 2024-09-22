package src;

import java.util.ArrayList;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class CommunicatorAgent extends Agent {
	private static final long serialVersionUID = 1L;
	private AID[] communicatorAgents;
	private boolean mayIStart = false;
	private ArrayList<String> iKnow;
	private static int doIHaveInfo;
	
	protected void setup () {
		Object[] args = getArguments();
		
		if ( args[1] != null ) doIHaveInfo = (int) args[1];
		
		System.out.println("Eu, " + this.getName() + ", devo transmitir informação? "+  doIHaveInfo);
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		
		ServiceDescription sd = new ServiceDescription();
		sd.setType("communicator");
		sd.setName("p2p-communicator");
		
		dfd.addServices(sd);	
		
		System.out.println("Eu sou: " + this.getName() + " e "+ this.getLocalName());
		
		iKnow = new ArrayList<String>();
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new ReceiveInitialMessage());
	}
	
	private class ReceiveInitialMessage extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;
		
		public void action () {
			//MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			
			//ACLMessage msg = myAgent.receive(mt);
			ACLMessage msg = myAgent.receive();
					
			if ( msg != null ) {
				
				if(msg.getPerformative() == ACLMessage.REQUEST) {
					
					String title = msg.getContent();
					
					System.out.println("Mensagem inicial recebida: " + title);
					
					ACLMessage reply = msg.createReply();
					
					reply.setPerformative(ACLMessage.CONFIRM);
					
					myAgent.send(reply);
					
					mayIStart = true;
					
					addBehaviour(new SendMessage());
				}
				
				if(msg.getPerformative() == ACLMessage.INFORM) {
					String title = msg.getContent();
					
					System.out.println("Mensagem de Hello recebida pelo " + myAgent.getName() + ": " + title);
				}
				
			}
			
		}
	}
	
//	private class ReceiveMessage extends CyclicBehaviour {
//		
//		private static final long serialVersionUID = 1L;
//		
//		public void action () {
//			while ( !mayIStart ) {
//				try {
//					Thread.sleep(2500);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
//			
//			ACLMessage msg = myAgent.receive(mt);
//					
//			if ( msg != null ) {
//				String title = msg.getContent();
//				
//				System.out.println("Mensagem de Hello recebida pelo " + myAgent.getName() + ": " + title);
//				
////				ACLMessage reply = msg.createReply();
////				
////				reply.setPerformative(ACLMessage.CONFIRM);
////				
////				myAgent.send(reply);
//			}
//			
//		}
//	}
	
	private class SendMessage extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		
		public void action () {
			while ( !mayIStart ) {
				try {
					Thread.sleep(2500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
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
				communicatorAgents = new AID[result.length];
				
				for (int i = 0; i < result.length; ++i) {
					communicatorAgents[i] = result[i].getName();
					System.out.println("ENCONTREI: " + communicatorAgents[i].getName());
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
			
			
			ACLMessage helloMessage = new ACLMessage(ACLMessage.INFORM);
			
			helloMessage.setConversationId("communicator");
			helloMessage.setContent("Hello from: " + myAgent.getName());
			
			for ( AID agent : communicatorAgents ) {
				if (! myAgent.getName().equals(agent.getName()) ) {
					//System.out.println("Enviando Mensagem de: "+myAgent.getName()+" para: " + agent.getName() + " igual? " + (myAgent.getName().equals(agent.getName())));
					helloMessage.addReceiver(agent);
				}
			}
			
			//addBehaviour(new ReceiveMessage());
			
			myAgent.send(helloMessage);
			
			
		}
	}
}
