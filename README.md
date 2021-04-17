# Sanwaf Framework Overview
 Sanwaf is a declarative data validation framework that secures your UI & Server without writing any code

- [Sanwaf-UI](https://github.com/bernardo1024/Sanwaf-UI) is a Sanitation Web Application Firewall that runs on the Browser
        
       - Uses a declarative mechanism to add validation to HTML pages
       - Add validation to a UI elements by including custom Sanwaf-UI Attributes
       - Fully configurable look and feel
       - No custom code is required to perform validation on web pages

-  [Sanwaf-Server](https://github.com/bernardo1024/Sanwaf-Server) is a Sanitation Web Application Firewall that runs on the Server
        
       - Sanwaf-Server secures parameters, cookies, headers and endpoints prior to entering your application code
       - Sanwaf-Server is configured with an XML file
       - Can be used independently of Sanwaf-UI
       - No custom code is required to perform validation on the server

- [Sanwaf-UI-2-Server](https://github.com/bernardo1024/Sanwaf-UI-2-Server) Utility converts the Sanwaf-UI declarative validation into the server XML format
        
       - Provides for effortless Sanwaf-Server configuration using Sanwaf-UI attributes
       - Converts the Sanwaf-UI declarative Attributes into a Sanwaf-Server consumable form
       - Automate Sanwaf-Server configuration using this utility

- [Sanwaf-Sample](https://github.com/bernardo1024/Sanwaf-Sample) project is a sample implementation of Sanwaf-UI and Sanwaf Server

       - End-2-end sample of using Sanwaf-UI & Sanwaf-Server
       - Dynamically configure and test Sanwaf-UI 
       - Dynamically disable Browser Validation and run against Server (uses embedded Jetty)

# SanWaf-UI-2-Server

Sanwaf-UI-2-Server is a utility that allows you to configure Sanwaf-Server using the Sanwaf-UI attributes added to web pages.  That is, you only have to run the utility to update the server configuration so that it will perform the same validation on the server as it did on the UI.

To run the Sanwaf-UI-2-Server utility:

1. Clone the project and run 
           
       mvn install 
        
3. Copy the sanwaf-ui-2-server.jar file from the target folder to a location on your build server
4. run the following command:

       java -jar sanwaf-ui-2-server.jar [path] [extensions] [file] [append] [output] [nonSanwaf] [endpoints] [strict]

       where (order of parameters not relevant):
        
          [path] 
              The root path from where to start recursively scanning for files to parse
              Format:  --path:<path>
              Example: --path:/path/to/files/

          [extensions]
              Comma separated list of file extension to search for
              Format:  --extensions:<list,of,extensions>
              Example: --extensions:.html,.jsp

          [file]
              Fully pathed filename to place outputs into
              Format:  --file:<pathed filename>
              Example: --file:/folder/sanwaf.xml

          [append]
              Flag to specify whether to append or override file
              Format:  --append:<true/false(default)>
              Example: --append:true

          [output]
              Flag to specify to output XML to console
              Format:  --output:<true/false(default)>
              Example: --output:true

          [nonSanwaf]
              Flag to specify to include non sanwaf elements as constants
              Format:  --nonSanwaf:<true/false(default)>
              Example: --nonSanwaf:true

          [endpoints]
              Flag to specify to use endpoints format in output
              Format:  --endpoints:<true/false(default)>
              Example: --endpoints:true

          [strict]
              Flag to include 'strict' attribute in output (only for doEndpoints)
              Format:  --strict:<true/false(default)/less>
              Example: --strict:less

          Note: When "--file" is specified, the file contents must include the following markers to place to generated XML:
            Start Marker: <!-- ~~~SANWAF-UI-2-SERVER-PLACEHOLDER-START~~~ -->
            End Marker:   <!-- ~~~SANWAF-UI-2-SERVER-PLACEHOLDER-END~~~ -->

          As the Sanwaf.xml file contains many sections, this controls where the output is placed
