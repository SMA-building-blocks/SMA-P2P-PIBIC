/*****************************************************************
JADE - Java Agent DEvelopment Framework is a framework to develop 
multi-agent systems in compliance with the FIPA specifications.
Copyright (C) 2000 CSELT S.p.A. 

GNU Lesser General Public License

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation, 
version 2.1 of the License. 

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 *****************************************************************/

package p2p_recommendation;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class ClientAgent extends Agent {
	// The title of the file to download
	private String targetFileTitle;
	// The list of known seeder agents
	private AID[] seederAgents;

	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hallo! Client-agent "+getAID().getName()+" is ready.");

		// Get the title of the file to download as a start-up argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			targetFileTitle = (String) args[0];
			System.out.println("Target file is "+targetFileTitle);

			// Add a TickerBehaviour that schedules a request to seeder agents every minute
			addBehaviour(new TickerBehaviour(this, 60000) {
				protected void onTick() {
					System.out.println("Trying to download "+targetFileTitle);
					// Update the list of seeder agents
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("file-seeder");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Found the following seeder agents:");
						seederAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							seederAgents[i] = result[i].getName();
							System.out.println(seederAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Perform the request
					myAgent.addBehaviour(new RequestPerformer());
				}
			} );
		}
		else {
			// Make the agent terminate
			System.out.println("No target file title specified");
			doDelete();
		}
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("CLient-agent "+getAID().getName()+" terminating.");
	}

	/**
	   Inner class RequestPerformer.
	   This is the behaviour used by client agents to request seeder 
	   agents the target file.
	 */
	private class RequestPerformer extends Behaviour {
		private AID bestSeeder; // The agent who provides the best seeding 
		private int bestSpeed;  // The best offered seeding
		private int repliesCnt = 0; // The counter of replies from seeder agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all seeders
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < seederAgents.length; ++i) {
					cfp.addReceiver(seederAgents[i]);
				} 
				cfp.setContent(targetFileTitle);
				cfp.setConversationId("file-requirement");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("file-requirement"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seeder agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						int speed = Integer.parseInt(reply.getContent());
						if (bestSeeder == null || speed > bestSpeed) {
							// This is the best offer at present
							bestSpeed = speed;
							bestSeeder = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= seederAgents.length) {
						// We received all replies
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the seeder that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeeder);
				order.setContent(targetFileTitle);
				order.setConversationId("file-requirement");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the download order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("file-requirement"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:      
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						System.out.println(targetFileTitle+" successfully downloaded file from agent "+reply.getSender().getName());
						System.out.println("Speed = "+bestSpeed);
						myAgent.doDelete();
					}
					else {
						System.out.println("Attempt failed: requested file already sold.");
					}

					step = 4;
				}
				else {
					block();
				}
				break;
			}        
		}

		public boolean done() {
			if (step == 2 && bestSeeder == null) {
				System.out.println("Attempt failed: "+targetFileTitle+" not available for sale");
			}
			return ((step == 2 && bestSeeder == null) || step == 4);
		}
	}  // End of inner class RequestPerformer
}
