package src;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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
import jade.wrapper.PlatformController;
import jade.wrapper.StaleProxyException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.tools.DummyAgent.DummyAgent;

public class SetupAgent extends Agent {
	
	
	// Instance variables
    //////////////////////////////////
    protected JFrame m_frame = null;
    protected Vector m_guestList = new Vector();    // invitees
    protected int m_guestCount = 0;                 // arrivals
    protected int m_rumourCount = 0;
    protected int m_introductionCount = 0;
    protected boolean m_partyOver = false;
    protected NumberFormat m_avgFormat = NumberFormat.getInstance();
    protected long m_startTime = 0L;

	private static final long serialVersionUID = 1L;
	private DFAgentDescription dfd;
	private AID[] communicatorAgents;
	
	private int netSize = 1;
	
	protected void setup () {
		Object[] args = getArguments();
		
		if ( args[0] != null ) netSize = Integer.valueOf(args[0].toString());
		
		System.out.println("TAMANHO DA REDE A SER CRIADA: "+  netSize);
		
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
		
		// add the GUI
        setupUI();

		
		
	}
	
	private class SetupEverything extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		
		public void action () {
			
			try {
	            System.out.println("The system is paused -- this action is only here to let you activate the sniffer on the agents, if you want (see documentation)");
	            System.out.println("Press enter in the console to start the agents");
	            System.in.read();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
			
			ACLMessage isAlive = new ACLMessage(ACLMessage.REQUEST);
			
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
	
	
    /**
     * Setup the UI, which means creating and showing the main frame.
     */
    private void setupUI() {
        m_frame = new HostUIFrame( this );

        m_frame.setSize( 400, 200 );
        m_frame.setLocation( 400, 400 );
        m_frame.setVisible( true );
        m_frame.validate();
    }
    
    protected void createPeers( int nPeers ) {
        // remove any old state
        m_guestList.clear();
        m_guestCount = 0;
        m_rumourCount = 0;
        m_introductionCount = 0;
        m_partyOver = false;
        ((HostUIFrame) m_frame).lbl_numIntroductions.setText( "0" );
        ((HostUIFrame) m_frame).prog_rumourCount.setValue( 0 );
        ((HostUIFrame) m_frame).lbl_rumourAvg.setText( "0.0" );

        // notice the start time
        m_startTime = System.currentTimeMillis();

        setNetworkState( "Inviting guests" );

        Runtime rt = Runtime.instance();
		
		Profile p = new ProfileImpl();
		
		ContainerController cc = rt.createAgentContainer(p);
        // PlatformController container = getContainerController(); // get a container controller for creating new agents
        // create N guest agents
        try {
            for (int i = 0;  i < nPeers;  i++) {
            	Object reference = new Object();
				
				Object arg[] = new Object[3];
				
				arg[0] = reference;
				arg[1] = 0;
				arg[2] = "";
				
				
				if ( i == (nPeers - 1) ) {
					arg[1] = 1;
					arg[2] = "eles brigaram 👀";
				}
				
                // create a new agent
				String localName = "peer_"+i;
				AgentController guest = cc.createNewAgent(localName, "src.CommunicatorAgent", arg);
				guest.start();
                //Agent guest = new GuestAgent();
                //guest.doStart( "guest_" + i );

                // keep the guest's ID on a local list
                m_guestList.add( new AID(localName, AID.ISLOCALNAME) );
            }
        }
        catch (Exception e) {
            System.err.println( "Exception while adding guests: " + e );
            e.printStackTrace();
        }
        addBehaviour(new SetupEverything());
    }
    
    /**
     * Update the state of the party in the UI
     */
    protected void setNetworkState( final String state ) {
        SwingUtilities.invokeLater( new Runnable() {
                                        public void run() {
                                            ((HostUIFrame) m_frame).lbl_networkState.setText( state );
                                        }
                                    } );
    }
}
