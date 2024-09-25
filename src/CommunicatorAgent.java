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
	private int doIHaveInfo;
	private String information;
	
	protected void setup () {
		Object[] args = getArguments();
		
		if ( args[1] != null ) {
			doIHaveInfo = (int) args[1];
			
			if ( args[2] != null ) information = (String) args[2];
		}
		
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
					
					if ( msg.getConversationId().equals("setup-agent") ) {
						String title = msg.getContent();
						
						System.out.println("Mensagem inicial recebida: " + title);
						
						ACLMessage reply = msg.createReply();
						
						reply.setPerformative(ACLMessage.CONFIRM);
						
						myAgent.send(reply);
						
						mayIStart = true;
						
						addBehaviour(new SendMessage());
					} else if ( msg.getConversationId().equals("gossip") ) {
						System.out.println("[DEBUG] Fofoca? 🐓☕");
						
						String msgContent = msg.getContent();
						
						System.out.println("Conteudo da fofoca: " + msgContent);
						
						ACLMessage replyMessage = msg.createReply();
						
						if ( doIHaveInfo > 0 ) {
							replyMessage.setPerformative(ACLMessage.CONFIRM);
							replyMessage.setContent(myAgent.getLocalName() + " responde: rapaz, ja me contaram...");
						} else {
							replyMessage.setPerformative(ACLMessage.REFUSE);
							replyMessage.setContent(myAgent.getLocalName() + " responde: sei nao, conta ai...");
						}
						
						myAgent.send(replyMessage);
					}
				}
				
				if ( msg.getPerformative() == ACLMessage.CONFIRM ) {
					System.out.println("[DEBUG] O agente " + msg.getSender().getLocalName() + " ja ta sabendo...");
				}
				
				if ( msg.getPerformative() == ACLMessage.REFUSE ) {
					System.out.println("[DEBUG] O agente " + msg.getSender().getLocalName() + " nao ta sabendo...");
					
					ACLMessage replyMessage = msg.createReply();
					
					replyMessage.setPerformative(ACLMessage.INFORM);
					replyMessage.setContent(myAgent.getLocalName() + " informa: " + information);
					
					myAgent.send(replyMessage);
					
				}
				
				if(msg.getPerformative() == ACLMessage.INFORM) {
					if ( msg.getConversationId().equals("setup-agent") ) {
						String title = msg.getContent();
						
						System.out.println("Mensagem de Hello recebida pelo " + myAgent.getName() + ": " + title);
					}
					
					if ( msg.getConversationId().equals("gossip") ) {
						System.out.println(msg.getContent());
						
						doIHaveInfo = 1;
						
						System.out.println("[DEBUG] Eu, " + myAgent.getLocalName() + ", to sabendo!");
					}
					
					
				}
				
			}
			
		}
	}
	
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
			
			searchAgents(template);
			
			ACLMessage helloMessage = new ACLMessage(ACLMessage.INFORM);
			
			helloMessage.setConversationId("communicator");
			helloMessage.setContent("Hello from: " + myAgent.getName());
			
			for ( AID agent : communicatorAgents ) {
				if (! myAgent.getName().equals(agent.getName()) ) {
					//System.out.println("Enviando Mensagem de: "+myAgent.getName()+" para: " + agent.getName() + " igual? " + (myAgent.getName().equals(agent.getName())));
					helloMessage.addReceiver(agent);
				}
			}
			
			myAgent.send(helloMessage);
			
			if ( doIHaveInfo > 0 ) {
				
				System.out.println("[DEBUG] Eu sou " + myAgent.getLocalName() + " e estou transmitindo informação pois tenho status: " + doIHaveInfo);
				
				ACLMessage askMessage = new ACLMessage(ACLMessage.REQUEST);
				
				askMessage.setConversationId("gossip");
				askMessage.setContent(myAgent.getName() + " pergunta: viu oq eles fizeram?");
				
				searchAgents(template);
				
				for ( AID agent : communicatorAgents ) {
					if (!myAgent.getName().equals(agent.getName()) ) {
						askMessage.addReceiver(agent);
					}
				}
				
				myAgent.send(askMessage);
			}
			
			
		}

		/**
		 * @param template
		 */
		private void searchAgents(DFAgentDescription template) {
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
		}
	}
}
