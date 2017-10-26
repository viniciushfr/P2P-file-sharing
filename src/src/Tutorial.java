/** *****************************************************************************
 *
 * "FreePastry" Peer-to-Peer Application Development Substrate
 *
 * Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute
 * for Software Systems.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of Rice  University (RICE), Max Planck Institute for Software
 * Systems (MPI-SWS) nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
 *
 * This software is provided by RICE, MPI-SWS and the contributors on an "as is"
 * basis, without any representations or warranties of any kind, express or implied
 * including, but not limited to, representations or warranties of
 * non-infringement, merchantability or fitness for a particular purpose. In no
 * event shall RICE, MPI-SWS or contributors be liable for any direct, indirect,
 * incidental, special, exemplary, or consequential damages (including, but not
 * limited to, procurement of substitute goods or services; loss of use, data, or
 * profits; or business interruption) however caused and on any theory of
 * liability, whether in contract, strict liability, or tort (including negligence
 * or otherwise) arising in any way out of the use of this software, even if
 * advised of the possibility of such damage.
 *
 ****************************************************************************** */
package src;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rice.environment.Environment;
import rice.p2p.commonapi.*;
import rice.p2p.commonapi.rawserialization.RawMessage;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.commonapi.PastryIdFactory;
import rice.pastry.direct.*;
import rice.pastry.leafset.LeafSet;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.RandomNodeIdFactory;

/**
 * This tutorial shows how to setup a FreePastry node using the Socket Protocol.
 *
 * @author Jeff Hoye
 */
public class Tutorial {

    // this will keep track of our applications
    //Vector<MyApp> apps = new Vector<MyApp>();
    /**
     * This constructor launches numNodes PastryNodes. They will bootstrap to an
     * existing ring if one exists at the specified location, otherwise it will
     * start a new ring.
     *
     * @param bindport the local port to bind to
     * @param bootaddress the IP:port of the node to boot from
     * @param numNodes the number of nodes to create in this JVM
     * @param env the environment for these nodes
     * @param useDirect true for the simulator, false for the socket protocol
     */
    Scanner scan = new Scanner(System.in);
    public Tutorial(int bindport, InetSocketAddress bootaddress, Environment env, boolean useDirect) throws Exception {

        // Generate the NodeIds Randomly
        NodeIdFactory nidFactory = new RandomNodeIdFactory(env);

        // construct the PastryNodeFactory
        PastryNodeFactory factory;
        if (useDirect) {
            NetworkSimulator<DirectNodeHandle, RawMessage> sim = new EuclideanNetwork<DirectNodeHandle, RawMessage>(env);
            factory = new DirectPastryNodeFactory(nidFactory, sim, env);
        } else {
            factory = new SocketPastryNodeFactory(nidFactory, bindport, env);
        }

        IdFactory idFactory = new PastryIdFactory(env);

        Object bootHandle = null;

        // construct a node, passing the null boothandle on the first loop will cause the node to start its own ring
        PastryNode node = factory.newNode();

        // construct a new FileApp
        FileApp app = new FileApp(node, idFactory);

        // boot the node
        node.boot(bootaddress);

        // the node may require sending several messages to fully boot into the ring
        synchronized (node) {
            while (!node.isReady() && !node.joinFailed()) {
                // delay so we don't busy-wait
                node.wait(500);

                // abort if can't join
                if (node.joinFailed()) {
                    throw new IOException("Could not join the FreePastry ring.  Reason:" + node.joinFailedReason());
                }
            }
        }

        System.out.println("Finished creating new node " + node);

        // wait 1 second
        env.getTimeSource().sleep(1000);

        // pick a node
        //FileApp app = apps.get(numNodes / 2);
        //PastryNode node = (PastryNode) app.getNode();
        // send directly to my leafset (including myself)
        LeafSet leafSet = node.getLeafSet();

        // select the item
        //NodeHandle nh = leafSet.get(i);
        // send the message directly to the node
        //app.sendMyMsgDirect(nh);
        
        int opcao = 1;
        while (opcao > 0) {
            System.out.println("1 - Procurar arquivo.");
            System.out.println("2 - Enviar arquivo para todos.");
            System.out.println("3 - Compartilhar arquivo");
            opcao = scan.nextInt();
            scan.nextLine();
            if (opcao == 1) {
                String mensagem = lerCatalogo();
                mensagem = "GET " + mensagem + " " + node.getId().toString();
                for (int i = -leafSet.ccwSize(); i <= leafSet.cwSize(); i++) {
                    //if(i!=0){
                    app.sendMyMsgDirect(leafSet.get(i), mensagem);
                    env.getTimeSource().sleep(100);
                    //}
                }
            } else if(opcao==2) {
               System.out.print("Nome do arquivo: ");
                String arquivo = scan.nextLine();
                for (int i = -leafSet.ccwSize(); i <= leafSet.cwSize(); i++) {
                    if (i != 0) {
                        app.sendMyFileDirect(leafSet.get(i), arquivo);
                        env.getTimeSource().sleep(100);
                    }
                }
            } else if(opcao==3){
                System.out.print("Caminho do artigo: ");
                String caminhoArquivo = scan.nextLine();
                System.out.print("Nome do arquivo: ");
                String nomeArquivo = scan.nextLine();
                System.out.print("Titulo do artigo: ");
                String tituloArtigo = scan.nextLine();
                System.out.print("Autor: ");
                String autor = scan.nextLine();
                System.out.print("Ano: ");
                String ano = scan.nextLine();
                try{
                    File f = new File(caminhoArquivo);
                    File f2 = new File(nomeArquivo);
                    f.renameTo(f2);
                    adicionarArtigoCatalago(nomeArquivo, tituloArtigo,autor,ano," "," ", " "," ");
                }catch(Exception e){
                     System.out.println("Erro ao mover arquivo " + e);
                     e.printStackTrace();
                }
                
                for (int i = -leafSet.ccwSize(); i <= leafSet.cwSize(); i++) {
                    if (i != 0) {
                        app.sendMyFileDirect(leafSet.get(i), "catalogo.xml");
                        env.getTimeSource().sleep(100);
                    }
                }
                
            }

        }

        // wait a bit
        env.getTimeSource().sleep(100);
    }
    public String lerCatalogo() {
        try {
            File xml = new File("catalogo.xml");

            DocumentBuilderFactory dbFactory2 = DocumentBuilderFactory.newInstance();
            DocumentBuilder db2 = dbFactory2.newDocumentBuilder();
            Document document = db2.parse(xml);

            NodeList list = document.getElementsByTagName("article");

            for (int i = 0; i < list.getLength(); i++) {
                org.w3c.dom.Node node = list.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {

                    Element eElement = (Element) node;
                    int x = i + 1;
                    System.out.println("Opção : " + x);
                    System.out.println("Title : " + eElement.getElementsByTagName("title").item(0).getTextContent());
                    System.out.println("Author : " + eElement.getElementsByTagName("author").item(0).getTextContent());
                    System.out.println("Year : " + eElement.getElementsByTagName("year").item(0).getTextContent());
                    System.out.println();
                }
            }

            System.out.print("Opção: ");
            int opcao = scan.nextInt();
            scan.nextLine();
            Element e = (Element) list.item(opcao - 1);
            return e.getElementsByTagName("file").item(0).getTextContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    public void adicionarArtigoCatalago(String file1,String titulo, String autor, String ano, String publicador, String paginas, String journal1, String v) throws ParserConfigurationException, SAXException, IOException{
        try {
            File xml = new File("catalogo.xml");

            DocumentBuilderFactory dbFactory2 = DocumentBuilderFactory.newInstance();
            DocumentBuilder db2 = dbFactory2.newDocumentBuilder();
            Document document = db2.parse(xml);

                                   
            Element root = (Element) document.getElementsByTagName("articles").item(0);
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                            
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
                            
            StreamResult result = new StreamResult(xml);
            
            Element article = document.createElement("article");
            root.appendChild(article);

            Element title = document.createElement("title");
            title.appendChild(document.createTextNode(titulo));
            article.appendChild(title);
            
            Element file = document.createElement("file");
            file.appendChild(document.createTextNode(file1));
            article.appendChild(file);

            Element author = document.createElement("author");
            author.appendChild(document.createTextNode(autor));
            article.appendChild(author);

            Element journal = document.createElement("journal");
            journal.appendChild(document.createTextNode(journal1));
            article.appendChild(journal);
            
            Element volume = document.createElement("volume");
            volume.appendChild(document.createTextNode(v));
            article.appendChild(volume);

            Element year = document.createElement("year");
            year.appendChild(document.createTextNode(ano));
            article.appendChild(year);

            Element publisher = document.createElement("publisher");
            publisher.appendChild(document.createTextNode(publicador));
            article.appendChild(publisher);
  
            
            transformer.transform(source, result);

            System.out.println("Done");
            
           
        } catch (Exception e) {
            e.printStackTrace();
        }
            
    }
    /**
     * Usage: java [-cp FreePastry-<version>.jar]
     * rice.tutorial.appsocket.Tutorial localbindport bootIP bootPort numNodes
     * or java [-cp FreePastry-<version>.jar] rice.tutorial.appsocket.Tutorial
     * -direct numNodes
     *
     * example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001
     * 10 example java rice.tutorial.DistTutorial -direct 10
     */
    public static void main(String[] args) throws Exception {
        try {

            boolean useDirect;
            if (args[0].equalsIgnoreCase("-direct")) {
                useDirect = true;
            } else {
                useDirect = false;
            }

            // Loads pastry settings
            Environment env;
            if (useDirect) {
                env = Environment.directEnvironment();
            } else {
                env = new Environment();

                // disable the UPnP setting (in case you are testing this on a NATted LAN)
                env.getParameters().setString("nat_search_policy", "never");
            }

            int bindport = 0;
            InetSocketAddress bootaddress = null;

            if (!useDirect) {
                // the port to use locally
                bindport = Integer.parseInt(args[0]);

                // build the bootaddress from the command line args
                InetAddress bootaddr = InetAddress.getByName(args[1]);
                int bootport = Integer.parseInt(args[2]);
                bootaddress = new InetSocketAddress(bootaddr, bootport);
            }

            // launch our node!
            Tutorial dt = new Tutorial(bindport, bootaddress, env, useDirect);
        } catch (Exception e) {
            // remind user how to use
            System.out.println("Usage:");
            System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.appsocket.Tutorial localbindport bootIP bootPort numNodes");
            System.out.println("  or");
            System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.appsocket.Tutorial -direct numNodes");
            System.out.println();
            System.out.println("example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001 10");
            System.out.println("example java rice.tutorial.DistTutorial -direct 10");
            throw e;
        }
    }
}
