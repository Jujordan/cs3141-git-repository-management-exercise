import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;

/**
 * TechAdventure is the main class for the Rat Attack HTML web game.
 * It launches a web server on a specified port and intakes user
 * commands, then responds accordingly.
 *
 * @author Logan Franke, Holden Kuempel, Justin Jordan, Ryan Tesch
 * Date Last Modified: 12/6/2024
 * 
 * CS1131, Fall 2024
 * Lab Section 02
 */
public class TechAdventure {
    int port = 2112; // Server port 2112 is usually free
    String prevOutcome = null;
    String room = null;
	 HttpServer server = null;

    // Rooms
    Room garage;
    Room kitchen;
    Room dining;
    Room masterBed;
    Room living;
    Room daughtersRoom;
    Room sonsRoom;
    Room foyer;
    Room bath;
    Room outside;
    Room bathWindow;

    // Player/Rat Object
    Rat player;

    // Items
    Food crumb;
    Food apple;
    Food bucketOfKFC;
    Item knife;
    Item newspaper;
    Item hat;

    // Chance of being spotted
    int spottedChance = 10; // Starts at a 10%
    int spottedDamage = 1;
    int spottedDecay = 2;
    int spottedIncrement = 7;
    boolean hasKilledHuman = false;

    // Save and Restore Stuff
    ArrayList<String> commandList = new ArrayList<>( );
    Scanner restoreScanner;
    String fileName = "SAVE FILE";
    File saveFile = new File( fileName );
    PrintWriter printWriter;
    boolean isRestoring = false;
    /**
     * The main method checks for a user specified port then starts the game
     * 
     * @param args - The cmd arguments
     */
    public static void main( String[ ] args ) { 
	TechAdventure techAdventure = new TechAdventure( );
	if ( args.length != 0 ) {
		char[ ] argChars = args[ 0 ].toCharArray( );
		String tempPort = "" + argChars[ 0 ] + argChars[ 1 ] + argChars[ 2 ] + argChars[ 3 ];
		techAdventure.port = Integer.parseInt( tempPort );
	}
        techAdventure.setUpGame( );
        techAdventure.launchServer( );
    }
    /**
     * This method launches the http server
     *
     */
    public void launchServer ( ) {
        try {
            server = HttpServer.create ( new InetSocketAddress 
						( port ), 0 );
            // Create a new context for handling resource requests
            HttpContext context = server.createContext ( "/" );
            context.setHandler ( this::handleRequest );
            // Accept connections on the specified port.
            server.start ( );
            System.out.printf( "Server started on port %s\n", port );
        } catch ( IOException e ) {
            e.printStackTrace ( );
        }
    }
    /**
     * This method handles requests from the http server
     *
     * @param httpExchange - the httpExchange for the server
     * @throws IOException
     */
    private void handleRequest( HttpExchange httpExchange ) throws IOException {
        // Retrieve the requested resource name
        URI uri = httpExchange.getRequestURI ( );
        if ( uri.getPath( ).equals( "/favicon.ico" ) ) {
            return;
        }
        room = uri.getPath( ).toUpperCase().substring( 1 );
        String query = uri.getQuery();
        Map<String, String> queryMap = parseQuery( query );
        String command = queryMap.get( "COMMAND" );
        // Compose the response
        String response = "Path: " + room + "\n";
        System.out.printf( "Request room: %s command: %s\n", room, command );
        switch( room ) {
            case "":
                response = generateLandingPage( );
                break;
            case "START":
                response = processCommand( room, command );
                break;
            case "GARAGE":
                response = processCommand( room, command );
                break;
            case "GAME":
					response = processCommand( room, command );
					break;
            case "KITCHEN":
                response = processCommand( room, command );
                break;
            default:
                response = "Bad Command: Given room is " + room;
        }
        // Send the response
        Headers h = httpExchange.getResponseHeaders ( );
        h.set( "Content−Type", "text/plain" );
        httpExchange.sendResponseHeaders( 200, response.length( ) );
        OutputStream os = httpExchange.getResponseBody ( );
        os.write( response.getBytes( ) );
        os.close( );
    }
    /**
     * This method uses html magic to format the command for the http server to understand
     *
     * @param query - The passed command/room
     * @return map - The web adress that the http server needs to show
     */
    private Map<String, String> parseQuery( String query ) {
        HashMap<String,String> map = new HashMap<>( );
        if (query != null) {
            System.out.printf( "query=%s\n", query );
            String[] queryParams = query.split("&");
            for (String param : queryParams) {
                String[] keyValue = param.split("=");
                String key = keyValue[0].toUpperCase( );
                String value = ( keyValue.length > 1 ? keyValue[1] : "" )
						 .toUpperCase( ).replace('+', ' ');
                map.put( key, value );
            }
        }
        return map;
    }
    /**
     * This method generates the first page the user will see when openning the game
     * 
     * @return response - The HTML code for the landing page in string form
     */
    private String generateLandingPage( ) {
        String response = "";
        response += "<p>";
        response += "Welcome to Rat Attack. You're not very sneaky on " +
			  "your own and you aren't stylish either. To make it worse " +
			  "your wife and kids are literally starving and John just " +
			  "ran off with a chicken bone down the driveway. You can " +
			  "feel the warmth rolling out of the crack in the garage wall " +
			  "and the smell of chicken with it. If you leave this house " +
			  "it better be with something...<br/>";
        response += "</p>";

        response += "<form action=\"GAME\">";
        response += " Type \"Start\" to start <input id=\"COMMAND\" name=\"COMMAND\" type=\"text\"<br/>";
        response += " <input id=\"SUBMIT\" name=\"SUBMIT\" value=\"SUBMIT\"type=\"submit\">";
        response += "</form>";
        return response;
    }
    /**
     * processCommand is possibly the heart of this program, as it takes in the
     * user command and changes the values of the game to progress it successfully.
     * 
     * @param room - The current room the player is in
     * @param command - The command the player has passed
     * 
     * @return result - The html code to show for the output of the command
     */
    private String processCommand( String room, String command ) {
        String result = "<p>Something terrible has happened if you're reading this</p>";
        if ( !( command.equals( "SAVE" ) ) 
				  && !( command.equals( "RESTORE" ) ) 
				  && !( command.equals( "QUIT" ) ) ) {
            commandList.add( command );
        }
        switch ( command ) {
            case "START":
                prevOutcome = "You begin the game!";
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "GO NORTH":
                if ( !( player.getRoom( ).canGoNorth( ) ) ) {
                    prevOutcome = "You cannot go north";
                    result = buildNewResponse( );
                    result += buildInput( );
                } else if ( playerSpotted( ) ) {
                    if ( ( player.hasNonFoodItem( ) ) 
								  && ( player.getNonFoodItem( )
									  .getName( )
									  .equals( knife.getName( ) ) ) ) {
                        hasKilledHuman = true;
                        player.getRoom( ).setPlayer( null );
                        player.moveToRoom( player.getRoom( )
										.getNorthernRoom( ) );
                        player.getRoom( ).setPlayer( player );
                        prevOutcome = "As you enter " + 
									player.getRoom( ).getRoomName( );
                        prevOutcome += "a human spots you, " + 
									"but you stab them with a knife.";
                        result = displayWin( );
                    } else {
                        boolean alive;
                        if ( isRestoring ) {
                            alive = true;
                        } else {
                            alive = player.takeDamage( spottedDamage );
                        }
                        if (alive) {
                            player.getRoom( ).setPlayer( null );
                            player.moveToRoom( player
											 .getRoom( )
											 .getNorthernRoom( ));
                            player.getRoom( ).setPlayer( player );
                            prevOutcome = "You enter the " + 
										 player.getRoom(  )
										 .getRoomName( ) + ", ";
                            prevOutcome += "but a Human swats you with " +
										 "a fly swatter. ";
                            prevOutcome += "You must eat otherwise the next " +
										 "time you are spotted you will be killed.";
                            room = player.getRoom().getRoomName().toUpperCase();
                            result = buildNewResponse();
                            result += buildInput();
                        } else {
                            prevOutcome = "As you attempt to enter " + 
										 player.getRoom()
										 .getNorthernRoom()
										 .getRoomName();
                            prevOutcome += " a human swats you with a " +
										 "fly swatter. ";
                            prevOutcome += "Your family watches as your " +
										 "body is thrown into the cold winter air";
                            prevOutcome += "Your family is forced to feed " + 
										 "upon your corpse as you failed " + 
										 "to provide for them. ";
                            result = "You lose ";
                        }
                    }
                } else {
                    player.getRoom( ).setPlayer( null );
                    player.moveToRoom( player.getRoom( ).getNorthernRoom( ) );
                    player.getRoom( ).setPlayer( player );
                    prevOutcome = "You enter the " + 
							  player.getRoom( )
							  .getRoomName( ) + ". ";
                    room = player.getRoom( ).getRoomName( ).toUpperCase( );
                    result = buildNewResponse( );
                    result += buildInput( );
                }
                break;
            case "GO EAST":
                if ( !( player.getRoom( ).canGoEast( ) ) ) {
                    prevOutcome = "You cannot go east";
                    result = buildNewResponse( );
                    result += buildInput( );
                } else if ( playerSpotted( ) ) {
                    if ( ( player.hasNonFoodItem( ) ) 
								  && ( player.getNonFoodItem( )
									  .getName( )
									  .equals( knife.getName( ) ) ) ) {
                        hasKilledHuman = true;
                        player.getRoom( ).setPlayer( null );
                        player.moveToRoom( player.getRoom( ).getEasternRoom( ) );
                        player.getRoom( ).setPlayer( player );
                        prevOutcome = "As you enter " + 
									player.getRoom( )
									.getRoomName( );
                        prevOutcome += "a human spots you, but you stab " +
									"them with a knife.";
                        result = displayWin( );
                    } else {
                        boolean alive;
                        if ( isRestoring ) {
                            alive = true;
                        } else {
                            alive = player.takeDamage(spottedDamage);
                        }
                        if (alive) {
                            player.getRoom().setPlayer(null);
                            player.moveToRoom(player.getRoom().getEasternRoom());
                            player.getRoom().setPlayer(player);
                            prevOutcome = "You enter the " + 
										 player.getRoom().getRoomName() + ", ";
                            prevOutcome += "but a Human swats you with a " +
										 "fly swatter. ";
                            prevOutcome += "You must eat otherwise the next " +
										 "time you are spotted you will be killed.";
                            room = player.getRoom().getRoomName().toUpperCase();
                            result = buildNewResponse();
                            result += buildInput();
                        } else {
                            prevOutcome = "As you attempt to enter " + 
										 player.getRoom().getEasternRoom().getRoomName();
                            prevOutcome += " a human swats you with a " +
										 "fly swatter. ";
                            prevOutcome += "Your family watches as your " +
										 "body is thrown into the cold winter air";
                            prevOutcome += "Your family is forced to feed " +
										 "upon your corpse as you failed " +
										 "to provide for them. ";
                            result = "You lose ";
                        }
                    }
                } else {
                    player.getRoom( ).setPlayer( null );
                    player.moveToRoom( player.getRoom( ).getEasternRoom( ) );
                    player.getRoom( ).setPlayer( player );
                    prevOutcome = "You enter the " + 
							  player.getRoom( ).getRoomName( ) + ".";
                    room = player.getRoom( ).getRoomName( ).toUpperCase( );
                    result = buildNewResponse( );
                    result += buildInput( );
                }
                break;
            case "GO SOUTH":
                if ( !( player.getRoom( ).canGoSouth( ) ) ) {
                    prevOutcome = "You cannot go south";
                } else if ( playerSpotted( ) ) {
                    if ( ( player.hasNonFoodItem( ) ) 
								  && ( player.getNonFoodItem( )
									  .getName( )
									  .equals( knife.getName( ) ) ) ) {
                        hasKilledHuman = true;
                        player.getRoom( ).setPlayer( null );
                        player.moveToRoom( player.getRoom( )
										.getSouthernRoom( ) );
                        player.getRoom( ).setPlayer( player );
                        prevOutcome = "As you enter " + 
									player.getRoom( ).getRoomName( );
                        prevOutcome += "a human spots you, but you " +
									"stab them with a knife.";
                        result = displayWin( );
                    } else {
                        boolean alive;
                        if ( isRestoring ) {
                            alive = true;
                        } else {
                            alive = player.takeDamage(spottedDamage);
                        }
                        if (alive) {
                            player.getRoom( ).setPlayer( null );
                            player.moveToRoom( player.getRoom( )
											 .getSouthernRoom( ) );
                            player.getRoom().setPlayer(player);
                            prevOutcome = "You enter the " + 
										 player.getRoom().getRoomName() + ", ";
                            prevOutcome += "but a Human swats you with " +
										 "a fly swatter. ";
                            prevOutcome += "You must eat otherwise the " +
										 "next time you are spotted you will be killed.";
                            room = player.getRoom().getRoomName().toUpperCase();
                            result = buildNewResponse();
                            result += buildInput();
                        } else {
                            prevOutcome = "As you attempt to enter " + 
										 player.getRoom().getSouthernRoom().getRoomName();
                            prevOutcome += " a human swats you with " +
										 "a fly swatter. ";
                            prevOutcome += "Your family watches as your " +
										 "body is thrown into the cold winter air";
                            prevOutcome += "Your family is forced to " +
										 "feed upon your corpse as you failed " +
										 "to provide for them. ";
                            result = "You lose ";
                        }
                    }
                } else {
                    player.getRoom( ).setPlayer( null );
                    player.moveToRoom( player.getRoom( ).getSouthernRoom( ) );
                    player.getRoom( ).setPlayer( player );
                    prevOutcome = "You enter the " + 
							  player.getRoom( ).getRoomName( ) + ".";
                    room = player.getRoom( ).getRoomName( ).toUpperCase( );
                    result = buildNewResponse( );
                    result += buildInput( );
                }
                break;
            case "GO WEST":
                if ( !( player.getRoom( ).canGoWest( ) ) ) {
                    prevOutcome = "You cannot go west";
                } else if ( playerSpotted( ) ) {
                    if ( ( player.hasNonFoodItem( ) ) 
								  && ( player.getNonFoodItem( )
									  .getName( )
									  .equals( knife.getName( ) ) ) ) {
                        hasKilledHuman = true;
                        player.getRoom( ).setPlayer( null );
                        player.moveToRoom( player.getRoom( ).getNorthernRoom( ) );
                        player.getRoom( ).setPlayer( player );
                        prevOutcome = "As you enter " + 
									player.getRoom( ).getRoomName( );
                        prevOutcome += "a human spots you, " +
									"but you stab them with a knife.";
                        result = displayWin( );
                    } else {
                        boolean alive;
                        if ( isRestoring ) {
                            alive = true;
                        } else {
                            alive = player.takeDamage(spottedDamage);
                        }
                        if (alive) {
                            player.getRoom().setPlayer(null);
                            player.moveToRoom(player.getRoom().getWesternRoom());
                            player.getRoom().setPlayer(player);
                            prevOutcome = "You enter the " + 
										 player.getRoom().getRoomName() + ", ";
                            prevOutcome += "but a Human swats " +
										 "you with a fly swatter. ";
                            prevOutcome += "You must eat otherwise the " +
										 "next time you are spotted you will be killed.";
                            room = player.getRoom().getRoomName().toUpperCase();
                            result = buildNewResponse();
                            result += buildInput();
                        } else {
                            prevOutcome = "As you attempt to enter " + 
										 player.getRoom().getWesternRoom().getRoomName();
                            prevOutcome += " a human swats you with " +
										 "a fly swatter. ";
                            prevOutcome += "Your family watches as your " +
										 "body is thrown into the cold winter air";
                            prevOutcome += "Your family is forced to feed " +
										 "upon your corpse as you failed to " +
										 "provide for them. ";
                            result = "You lose ";
                        }
                    }
                } else {
                    player.getRoom( ).setPlayer( null );
                    player.moveToRoom( player.getRoom( ).getWesternRoom( ) );
                    player.getRoom( ).setPlayer( player );
                    prevOutcome = "You enter the " + 
							  player.getRoom( ).getRoomName( ) + ".";
                    room = player.getRoom( ).getRoomName( ).toUpperCase( );
                    result = buildNewResponse( );
                    result += buildInput( );
                }
                break;
            case "PICK UP NEWSPAPER":
                if ( player.getRoom( ).containsItem( newspaper ) ) {
                    prevOutcome = "You picked up Newspaper, you can " +
							  "no longer be spotted! ";
                    pickUp( newspaper, player, player.getRoom( ) );
                } else {
                    prevOutcome = "Newspaper is not in this room. Try again! ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "DROP NEWSPAPER":
                if ( ( player.hasNonFoodItem( ) ) 
							 && ( player.getNonFoodItem( )
								 .getName( )
								 .equals( newspaper.getName( ) ) ) ) {
                    prevOutcome = "You dropped Newspaper. ";
                    drop( newspaper, player, player.getRoom( ) );
                } else {
                    prevOutcome = "You do not have Newspaper. ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "PICK UP KNIFE":
                if ( player.getRoom( ).containsItem( knife ) ) {
                    prevOutcome = "You picked up Knife, if you are " +
							  "spotted you will instead attack! ";
                    pickUp( knife, player, player.getRoom( ) );
                } else {
                    prevOutcome = "Knife is not in this room. Try again! ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "DROP KNIFE":
                if ( ( player.hasNonFoodItem( ) ) 
							 && ( player.getNonFoodItem( )
								 .getName( )
								 .equals( knife.getName( ) ) ) ) {
                    prevOutcome = "You dropped Knife";
                    drop( knife, player, player.getRoom( ) );
                } else {
                    prevOutcome = "You do not have knife. ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "PICK UP HAT":
                if ( player.getRoom( ).containsItem( hat ) ) {
                    prevOutcome = "You picked up hat, you now " +
							  "score additional points! ";
                    pickUp( hat, player, player.getRoom( ) );
                } else {
                    prevOutcome = "Hat is not in this room. Try again! ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "DROP HAT":
                if ( ( player.hasNonFoodItem( ) ) 
							 && ( player.getNonFoodItem( )
								 .getName( )
								 .equals( hat.getName( ) ) ) ) {
                    prevOutcome = "You dropped Hat! Hats off to you!";
                    drop( hat, player, player.getRoom( ) );
                } else {
                    prevOutcome = "You do not have hat. ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "PICK UP CRUMB":
                if ( player.getRoom( ).containsItem( crumb ) ) {
                    prevOutcome = "You picked up crumb, you can " +
							  "eat this (or \"win\" with this)! ";
                    pickUp( crumb, player, player.getRoom( ) );
                } else {
                    prevOutcome = "Crumb is not in this room. Try again! ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "DROP CRUMB":
                if ( ( player.hasFoodItem( ) ) 
							 && ( player.getFoodItem( )
								 .getName( )
								 .equals( crumb.getName( ) ) ) ) {
                    prevOutcome = "You dropped Crumb";
                    drop( crumb, player, player.getRoom( ) );
                } else {
                    prevOutcome = "You do not have crumb. ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "PICK UP APPLE":
                if ( player.getRoom( ).containsItem( apple ) ) {
                    prevOutcome = "You picked up apple, you can eat " +
							  "this (or win with this)! ";
                    pickUp( apple, player, player.getRoom( ) );
                } else {
                    prevOutcome = "Apple is not in this room. Try again! ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "DROP APPLE":
                if ( ( player.hasFoodItem( ) ) 
							 && ( player.getFoodItem( )
								 .getName( )
								 .equals( apple.getName( ) ) ) ) {
                    prevOutcome = "You dropped Apple";
                    drop( apple, player, player.getRoom( ) );
                } else {
                    prevOutcome = "You do not have apple. ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "PICK UP BUCKET OF KFC":
                if ( player.getRoom( ).containsItem( bucketOfKFC ) ) {
                    prevOutcome = "You picked up the bucket of KFC, " +
							  "you can eat this (or WIN with this)! ";
                    pickUp( bucketOfKFC, player, player.getRoom( ) );
                } else {
                    prevOutcome = "Bucket Of KFC is not in this " +
							  "room. Try again! ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "DROP BUCKET OF KFC":
                if ( ( player.hasFoodItem( ) ) 
							 && ( player.getFoodItem( )
								 .getName( )
								 .equals( bucketOfKFC.getName( ) ) ) ) {
                    prevOutcome = "You dropped the bucket of KFC";
                    drop( bucketOfKFC, player, player.getRoom( ) );
                } else {
                    prevOutcome = "You do not have the bucket of KFC ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "EAT CRUMB":
                if ( ( player.hasFoodItem( ) ) 
							 && ( player.getFoodItem( )
								 .getName( )
								 .equals( crumb.getName( ) ) ) ) {
                    player.getFoodItem( ).eat( );
                    prevOutcome = "You ate the crumb. If you get " +
							  "spotted again you will be safe. ";
                } else {
                    prevOutcome = "You do not have the crumb. ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "EAT APPLE":
                if ( ( player.hasFoodItem( ) ) 
							 && ( player.getFoodItem( )
								 .getName( )
								 .equals( apple.getName( ) ) ) ) {
                    player.getFoodItem( ).eat( );
                    prevOutcome = "You ate the apple. If you get " +
							  "spotted again you will be safe. ";
                } else {
                    prevOutcome = "You do not have the apple. ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "EAT BUCKET OF KFC":
                if ( ( player.hasFoodItem( ) ) 
							 && ( player.getFoodItem( )
								 .getName( )
								 .equals( bucketOfKFC.getName( ) ) ) ) {
                    player.getFoodItem( ).eat( );
                    prevOutcome = "You ate the Bucket of KFC. If you " +
							  "get spotted again you will be safe. " +
                            "But how could you eat the whole thing??? " +
									 "Now your kids will certainly starve.";
                } else {
                    prevOutcome = "You do not have the Bucket of KFC. ";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "LOOK CRUMB":
                if ( ( player.hasFoodItem( ) ) 
							 && ( player.getFoodItem( )
								 .getName( )
								 .equals( crumb.getName( ) ) ) ) {
                    prevOutcome = "You look at crumb, " + 
							  player.getFoodItem().getDescription();
                } else {
                    prevOutcome = "You do not have crumb.";
                }
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "LOOK APPLE":
					 if ( ( player.hasFoodItem( ) ) 
							 && ( player.getFoodItem( )
								 .getName( )
								 .equals( apple.getName( ) ) ) ) {
					 	prevOutcome = "You look at the Apple, " + 
							player.getFoodItem( ).getDescription( );
					 } else { 
					 	prevOutcome = "You do not have apple.";
					 }
					 result = buildNewResponse( );
					 result += buildInput( );
					 break;
            case "LOOK BUCKET OF KFC":
            	if ( ( player.hasFoodItem( ) ) 
							&& ( player.getFoodItem( )
								.getName( )
								.equals( bucketOfKFC.getName( ) ) ) ) {
						prevOutcome = "You look at the Bucket of KFC, " + 
							player.getFoodItem( ).getDescription( );
					} else {
						prevOutcome = "You do not have a Bucket of KFC.";
					}
					result = buildNewResponse( );
					result += buildInput( );
					break;
			   case "LOOK NEWSPAPER":
            	if ( ( player.hasNonFoodItem( ) ) 
							&& ( player.getNonFoodItem( )
								.getName( )
								.equals( newspaper.getName( ) ) ) ) {
						prevOutcome = "You read the newspaper, " + 
							player.getNonFoodItem( ).getDescription( );
					} else { 
						prevOutcome = "You do not have a newspaper.";
					}
					result = buildNewResponse( );
					result += buildInput( );
					break;
			   case "LOOK KNIFE":
                if ( ( player.hasNonFoodItem( ) )
							 && ( player.getNonFoodItem( )
								 .getName( )
								 .equals( knife.getName( ) ) ) ) {
						 prevOutcome = "You inspect the knife, " +
							 player.getNonFoodItem( ).getDescription( );
					 } else {
						 prevOutcome = "You do not have a knife.";
					 }
					 result = buildNewResponse( );
					 result += buildInput( );
					 break;
            case "LOOK HAT":
            	if ( ( player.hasNonFoodItem( ) )
							&& ( player.getNonFoodItem( )
								.getName( )
								.equals( hat.getName( ) ) ) ) {
						prevOutcome = "You inspect the hat, " +
							player.getNonFoodItem( ).getDescription( );
					} else {
						prevOutcome = "You do not have a hat.";
					}
					result = buildNewResponse( );
					result += buildInput( );
					break;
				case "SAVE":
                try {
                    printWriter = new PrintWriter( fileName );
                    printWriter.println( player.getHitPoints( ) );
                    for ( String c : commandList ) {
                        printWriter.println( c );
                    }
                    printWriter.close( );
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                prevOutcome = "You have successfully saved!";
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "RESTORE":
                try {
                    restoreScanner = new Scanner( saveFile );
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                processCommand( room, "RESTART" );
                if ( restoreScanner.hasNextInt( ) ) {
                    player.setHitPoints( restoreScanner.nextInt( ) );
                }
                while ( restoreScanner.hasNextLine( ) ) {
                    isRestoring = true;
                    processCommand( room, restoreScanner.nextLine( ) );
                }
                isRestoring = false;
                prevOutcome = "You have successfully restored! Continue from you last save!";
                result = buildNewResponse( );
                result += buildInput( );
                break;
            case "QUIT":
					 server.stop( 0 );
					 break;
            case "RESTART":
                setUpGame( );
                prevOutcome = "You have successfully restarted!";
                result = buildNewResponse( );
                result += buildInput( );
                break;
            default:
                prevOutcome = "Invalid Input, try again!";
                result = buildNewResponse( );
                result += buildInput( );
                break;
        }
		  if ( player.getRoom( ).getRoomName( ).equals( outside.getRoomName( ) ) ) {
		  		result = displayWin( );
		  } else if ( player.getRoom( ).getRoomName( ).equals( bathWindow.getRoomName( ) ) ) {
              result = displayWin( );
          }
        return result;
    }

    /**
     * This method will operate procedures necessary for the beginning of the game.
     *
     */
    public void setUpGame( ) {
        this.setUpVariables( );
        this.roomSetUp( );
        this.playerSetUp( );
        this.itemSetUp( );
    }

    /**
     * This method will assign and connect rooms!
     *
     */
    public void roomSetUp( ) {
        // Room Garage
        String garageName = "Garage";
        String garageDescription = "The Garage is messy containing a car, " +
				"and a workshop that is covered with tools, scrap wood, and other " +
				"nicks and nacks. There is a latter resting against the wall. " +
				"In the corner, you see a piece of cheese. To the East, you see a " + 
				"door into the Kitchen, and to the South, you see that the garage " +
				"door is open leading to Freedom.";
        garage = new Room( garageName, garageDescription );

        // Room Kitchen
        String kitchenName = "Kitchen";
        String kitchenDescription = "The Kitchen is pristine with a large " +
				"countertop space and an island. In the corner, you see a loose " +
				"piece of cheese. On the cuontertop, you see a variety of kitchen " +
				"knives. To the north, you see an opening leading to the Dining " +
				"Room. To the South, there is a door leading to the Bathroom. " +
				"To the West, you see a door that leads to the Garage.";
        kitchen = new Room( kitchenName, kitchenDescription );

        // Room Dining Room
        String diningName = "Dining Room";
        String diningDescription = "The Dining Room is clean, but also " +
			  "lived in. There is a large table with chairs, and a large, " +
			  "closed, window. On the table, there is a large bucket of " +
			  "KFC chicken. To the South, there is an opening leading " +
			  "to the Kitchen. To the West, there is a door leading " +
			  "to the Master Bedroom.";
        dining = new Room( diningName, diningDescription );

        // Room master Bedroom
        String masterName = "Master Bedroom";
        String masterDescription = "The Master Bedroom is clean and tidy " +
			  "on one side and messy and in disarray on the other. The " +
			  "bed takes up most of the room, and there are two " +
			  "nightstands, one on either side. The nightstands each " +
			  "have different items atop them. To the North, there " +
			  "is a large open window leading to the Outside. To the " +
			  "East, there is a door leading to the Dining Room.";
        masterBed = new Room( masterName, masterDescription );

        // Room Living Room
        String livingName = "Living Room";
        String livingDescription = "The Living Room is unkept and " +
			  "well-loved. There is a large mantle with a TV atop it, " +
			  "a large couch, two reclining armchairs, and a coffee " +
			  "table. To the North, There is a door to the Daughter's " +
			  "Bedroom. To the East, there is a door to the Son's " +
			  "Bedroom. To the South, there is an opening to the " +
			  "Foyer. To the West, there is another opening " +
			  "leading to the Kitchen.";
        living = new Room( livingName, livingDescription );

        // Room Daughter's Bedroom
        String daughterRoomName = "Daughter's Bedroom";
        String daughterRoomDescription = "The Daughter's Bedroom is tidy " +
			  "and in disarray. Stuffed animals are littered throughout " +
			  "the room. There is a small bed that seems to be more " +
			  "plus than bed. To the East, there also appears to be " + 
			  "an open window. To the South, there is a door leading " +
			  "to the Living Room.";
        daughtersRoom = new Room( daughterRoomName, daughterRoomDescription );

        // Room Son's Room
        String sonRoomName = "Son's Bedroom";
        String sonRoomDescription = "The Son's Bedroom is messy with " +
			  "laundry and trash scattered across the room. There is a " +
			  "small, messy, unmade bed. To the East, there is an " +
			  "open window leading outside. To the West, there is a " +
			  "door leading to the Living Room.";
        sonsRoom = new Room( sonRoomName, sonRoomDescription );

        // Room Foyer
        String foyerName = "Foyer";
        String foyerDescription = "The Foyer is small and simple with a " +
			  "rug and a closet. To the North, there is an opening to " +
			  "the Living Room. To the South, there is a door to the Outside.";
        foyer = new Room( foyerName, foyerDescription );

        // Room Bathroom
        String bathName = "Bathroom";
        String bathDescription = "The Bathroom is clean. The bathroom " +
			  "has a sink, toilet, and shower. The bathroom window is " +
			  "slightly ajar. To the North, there is a door leading " +
			  "to the Kitchen. To the South, there is a window that is " +
			  "slightly ajar leading to the Outside.";
        bath = new Room( bathName, bathDescription );
		
        // Room Outside
        String outsideName = "Outside";
        String outsideDescription = "The great oudoors";
        outside = new Room( outsideName, outsideDescription );

        // Room fallDamage
        String bathWindowName = "Bathroom Window";
        String bathWindowDescription = "A large open window with a " +
			  "steep drop off.";
        bathWindow = new Room( bathWindowName, bathWindowDescription );

        // Connections between Rooms
        garage.setEasternRoom( kitchen );
        garage.setSouthernRoom( outside );
        kitchen.setNorthernRoom( dining );
        kitchen.setEasternRoom( living );
        kitchen.setSouthernRoom( bath );
        kitchen.setWesternRoom( garage );
        dining.setSouthernRoom( kitchen );
        dining.setWesternRoom( masterBed );
        masterBed.setEasternRoom( dining );
        masterBed.setNorthernRoom( outside );
        living.setNorthernRoom( daughtersRoom );
        living.setEasternRoom( sonsRoom );
        living.setSouthernRoom( foyer );
        living.setWesternRoom( kitchen );
        daughtersRoom.setSouthernRoom( living );
        daughtersRoom.setEasternRoom( outside );
        sonsRoom.setWesternRoom( living );
        sonsRoom.setEasternRoom( outside );
        foyer.setNorthernRoom( living );
        foyer.setSouthernRoom( outside );
        bath.setNorthernRoom( kitchen );
        bath.setSouthernRoom( bathWindow );
    }
    /**
     * The method to set the player to a rat class
     *
     */
    public void playerSetUp( ) {
        player = new Rat( garage );
    }
    /**
     * This method prints out all the necessary info for the room you are in,
     * including room description, inventort, and other availible actions
     *
     * @return result - The resulting HTML code buildup
     */
    public String buildNewResponse( ) {
	 	Room currRoom = player.getRoom( );
		String response = "<p>" + prevOutcome + "</p>";
		response += "<b>ROOM: " + player.getRoom( ).getRoomName( ) + "</b>";
		response += "<p>" + currRoom.getRoomDescription( ) + "</p>";
		
		response += "<p>Adjacent Rooms:<br/>";
		if ( currRoom.canGoNorth( ) ) {
			response += "North: " + currRoom.getNorthernRoom( ).getRoomName( ) + "<br/>";
		} if ( currRoom.canGoEast( ) ) {
			response += "East: " + currRoom.getEasternRoom( ).getRoomName( ) + "<br/>";
		} if ( currRoom.canGoSouth( ) ) {
			response += "South: " + currRoom.getSouthernRoom( ).getRoomName( ) + "<br/>";
		} if ( currRoom.canGoWest( ) ) {
			response += "West: " + currRoom.getWesternRoom( ).getRoomName( ) + "<br/>";
		}
        if ( currRoom.containsItems( ) ) {
            response += "Items: ";
            for ( Item item : currRoom.getAllItems( ) ) {
                response += item.getName( ) + ", ";
            }
            response += "<br/>";
        }
		response += "</p>";

        // Hit Points
        response += "<b>HIT POINTS:</b>";
        response += "<p>";
        response += "Hit Points: " + player.getHitPoints( ) + " / " + player.getHitPointMax( );
        response += "</p>";

        // Inventory
        response += "<b>INVENTORY:</b>";
        response += "<p>";
         if ( player.hasFoodItem( ) ) {
             response += player.getFoodItem( ).getName( ) + "<br/>";
         }
         if ( player.hasNonFoodItem( ) ) {
             response += player.getNonFoodItem( ).getName( ) + "<br/>";
         }
         response += "</p>";

		response += "<b>VALID ACTIONS:</b>";
		response += "<p>Quit (End the game)<br/>Save (Save your file)<br/>" +
			"Restore (Restore a saved game)<br/>";
		
		// Room Actions
		if ( currRoom.canGoNorth( ) ) {
			response += "Go North<br/>";
		} if ( currRoom.canGoEast( ) ) {
			response += "Go East<br/>";
		} if ( currRoom.canGoSouth( ) ) {
			response += "Go South<br/>";
		} if ( currRoom.canGoWest( ) ) {
			response += "Go West<br/>";
		}

        // Object Actions
         if ( player.hasItem( ) ) {
             if ( player.hasFoodItem( ) ) {
                 response += "Eat " + player.getFoodItem( ).getName( ) + "<br/>";
                 response += "Drop " + player.getFoodItem( ).getName( ) + "<br/>";
					  response += "Look " + player.getFoodItem( ).getName( ) + "<br/>";
             }
             if ( player.hasNonFoodItem( ) ) {
                 response += "Drop " + player.getNonFoodItem( ).getName( ) + "<br/>";
					  response += "Look " + player.getNonFoodItem( ).getName( )+ "<br/>";
             }
         }
         if ( currRoom.containsItems( ) ) {
             for ( Item item : currRoom.getAllItems( ) ) {
                 response += "Pick Up " + item.getName( ) + "<br/>";
             }
         }
		
		response += "</p>";
		
		return response;
	 }
         /**
	  * This method builds the input box code for the game
          *
	  * @return result - The HTML code for the input area
          */
	 public String buildInput( ) {
        String result = "<form action=\"GAME\">";
        result += " What will you do? <input id=\"COMMAND\" name=\"COMMAND\" type=\"text\"<br/>";
        result += " <input id=\"SUBMIT\" name=\"SUBMIT\" value=\"SUBMIT\" type=\"submit\">";
        result += "</form>";
		return result;
	 }
     /**
      * This method sets up all the items in the game
      * 
      */
     public void itemSetUp( ) {
        // Sets the values for the items
        // Items should be implemented!
        crumb = new Food( "Crumb", "Crumb description", "SMALL" );
        apple = new Food( "Apple", "Apple description", "MEDIUM" );
        bucketOfKFC = new Food( "Bucket of KFC", "Bucket of KFC description", "LARGE" );
        knife = new Item( "Knife", "WEAPON", "Knife description", 50 );
        newspaper = new Item( "Newspaper", "CLOAK", "Newspaper description", 0 );
        hat = new Item( "Hat", "HAT", "Hat description", 100 );

		  crumb.setDescription( "The age-old crumb, for humans, is something " +
				  "insignificant and forgotten fallen onto the ground and " +
				  "collecting dust on the floor awaiting its day to be swept " +
				  "up into the trash. But for a rat, it is plenty of food " +
				  "for a full-grown rat to survive through an easy winter. " +
				  "Eating it now will give me a boost for the next time " + 
				  "I meet the human." );

		  apple.setDescription( "The fresh fruit of the tree. Think of a " +
				  "hot summer day with a glass of lemonade and a crisp " +
				  "green apple with a singular drop of condensation down " +
				  "the side. You think “ This Apple can get me and my " +
				  "wife comfortable through the winter (the kids can " +
				  "fend for themselves) “. Eating this will give you " +
				  "the strength to continue on after seeing the " +
				  "ravenous beast known as a human." );

		  bucketOfKFC.setDescription( "The aroma of the 11 herbs and " +
				  "spices fills the air as you feel yourself float " +
				  "through the air following the smell. You snap out " +
				  "of it remembering that this bucket will make you and " +
				  "your family the richest rats in all of the tri-state " +
				  "area. Although it feels like a waste you can tell " +
				  "that eating this will give you the chance to " +
				  "keep fighting with the humans." );

		  knife.setDescription( "Factory new Case hardened Karambit with " +
				  "a 387 pattern in Blue Gem. Glistening in all its glory " +
				  "so sharp it's a 0 on the sharpness scale with it you " +
				  "may be able to fight back against the nasty, " +
				  "horrible, and abominable Human." );

		  newspaper.setDescription( " \"You're the bonnie to my clyde " +
				  "says Biden running off with Pardoned Turkey\" states " +
				  "the headline. Better off just using this as cover " +
				  "instead of reading it." );

		  hat.setDescription( "The \"Hat\" was known throughout the ages as " +
				  "the focal point of high-brow fashion even the common " +
				  "folk tried to copy its style but with all the hats, " +
				  "caps, breton, hennin, and bowlers compared to this one " +
				  "singular hat with its glistening brim, sleek design, " +
				  "and leather so good it could be sold on the black " +
				  "market for over billions of dollars. With it on you " +
				  "look BadA** and so cool women fall over at the sight of you." );

        // Establishes the locations of the items
         crumb.setRoom( masterBed );
         masterBed.addItem( crumb );
         apple.setRoom( dining );
         dining.addItem( apple );
         bucketOfKFC.setRoom( daughtersRoom );
         daughtersRoom.addItem( bucketOfKFC );
         knife.setRoom( kitchen );
         kitchen.addItem( knife );
         newspaper.setRoom( garage );
         garage.addItem( newspaper );
         hat.setRoom( bath );
         bath.addItem( hat );
     }
     /**
      * Allows the player to pick up an item
      *
      * @param item - The item to pick up
      * @param player - The player object
      * @param room - The room the player is in
      */
     public void pickUp( Item item, Rat player, Room room ) {
        room.removeItem( item.getName( ) );
        item.setRoom( null );
        if ( item.getType( ).equals( "FOOD" ) ) {
            if ( player.hasFoodItem( ) ) {
                drop( player.getFoodItem( ), player, player.getRoom( ) );
            }
            player.addToInv( item );
        } else {
            if ( player.hasNonFoodItem( ) ) {
                drop( player.getNonFoodItem( ), player, player.getRoom( ) );
            }
            player.addToInv( item );
        }
        item.setPlayer( player );
     }
     /**
      * Allows the player to drop an item
      *
      * @param item - The item to drop
      * @param player - The player object
      * @param room - The room the player is in
      */
     public void drop( Item item, Rat player, Room room ) {
        if ( item.getType( ).equals( "FOOD" ) ) {
            player.removeFoodFromInv( );
        } else {
            player.removeNonFoodFromInv( );
        }
        item.setPlayer( null );
        item.setRoom( room );
        room.addItem( item );
     }
     /**
      * Checks if the player has been spotted
      *
      * @return - True or false is the player was or wasn't spotted
      */
     public boolean playerSpotted( ) {
        Random random = new Random( );
        int chanceRolled = random.nextInt( 99 ) + 1;
        if ( chanceRolled <= spottedChance ) {
            if ( player.hasNonFoodItem( ) ) {
                if ( !( player.getNonFoodItem( ).getName( ).equals( newspaper.getName( ) ) ) ) {
                    spottedChance /= spottedDecay;
                    return true;
                } else {
                    spottedChance += spottedIncrement;
                    return false;
                }
            } else {
                spottedChance /= spottedDecay;
                return true;
            }
        } else {
            spottedChance += spottedIncrement;
            return false;
        }
     }
     /**
      * This method builds the win screen of the game
      * 
      * @return victoryText - The HTML code of the win screen
      */
     public String displayWin( ) {
         String victoryText = "<b>You ";
         int score = 0;
         if ( player.getRoom( ).getRoomName( ).equals( bathWindow.getRoomName( ) ) ) {
             victoryText += "lose.</b>";
             victoryText += "<p>You fell from the Bathroom window as you attempted to escape. Your remains splatter on the ground.</p>";
         } else {
             if (player.hasFoodItem()) {
                 victoryText += "Win!</b>";
                 score += player.getFoodItem().getScoreValue();
                 switch (player.getFoodItem().getName()) {
                     case "Crumb":
                         victoryText += "<p>You got a crumb, saving yourself," +
                                 " but leaving your family to die.</p>";
                         break;
                     case "Apple":
                         victoryText += "<p>You retrieved an apple, which is " +
                                 "enough food for you and your wife to survive the " +
                                 "winter, but your children brutally freeze to death " +
                                 "in the harsh winter.</p>";
                         break;
                     case "Bucket of KFC":
                         victoryText += "<p>You escaped with a bucket of KFC Crisy " +
                                 "fried chicken, with all its 11 herbs and spices. " +
                                 "Now <b>THAT'S</b> finger lickin' good! (not sponsored). " +
                                 "Your family is fed well, except for your vegitarian son Timmy " +
                                 "who was too picky to eat. Disappointing.</p>";
                         break;
                     default:
                         victoryText += "<p>You successfully bugged the game</p>";
                         break;
                 }
             } else {
                 victoryText += " Lost!</b><p>You go back to your family empty " +
                         "handed. Your wife gives you a cold disappointed look. " +
                         "\"I knew you couldn't do it. I should have left " +
                         "for John the Rat when I had the chance.\". Your wife " +
                         "spits at you and runs away with the children likely to try to " +
                         "go to John. As the winter goes on, you " +
                         "to starve. What have you done?</p>";
             }
             if (player.hasNonFoodItem( ) ) {
                 score += player.getNonFoodItem().getScoreValue();
                 switch (player.getNonFoodItem().getName()) {
                     case "Hat":
                         victoryText += "<p>Not only that, but you looked so sexy " +
                                 "while doing it. +5 children.</p>";
                         break;
                     case "Newspaper":
                         victoryText += "<p>However, you are a coward for hiding under " +
                                 "a newspaper. Your wife picked up a side dude because you " +
                                 "were no longer manly enough.</p>";
                         break;
                     case "Knife":
                         break;
                     default:
                         victoryText += "<p>You successfully bugged the game.</p>";
                         break;
                 }
             }
             if (hasKilledHuman) {
                 victoryText = "<b>You win! As you get spotted this time " +
						  "you stab the human instead of being stomped out " +
						  "and your rat family can move " +
						  "in and eat ALLL of the food! </b>";
             }
         }
        victoryText += "<b>Your Score: " + score + "!</b>";
        return victoryText;
     }
     /**
      * This method sets up the necessary variables for the game
      *
      */
     public void setUpVariables( ) {
         prevOutcome = null;
         room = null;

         // Chance of being spotted
         spottedChance = 10; // Starts at a 10%
         spottedDamage = 1;
         spottedDecay = 2;
         spottedIncrement = 7;
         hasKilledHuman = false;

         // Save and Restore Stuff
         commandList = new ArrayList<>( );
         fileName = "SAVE FILE";
         saveFile = new File( fileName );
	}
     }


