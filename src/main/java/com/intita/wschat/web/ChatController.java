package com.intita.wschat.web;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpRequest;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.DeferredResult;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intita.wschat.config.CustomAuthenticationProvider;
import com.intita.wschat.domain.ChatMessage;
import com.intita.wschat.domain.SessionProfanity;
import com.intita.wschat.event.LoginEvent;
import com.intita.wschat.event.ParticipantRepository;
import com.intita.wschat.exception.TooMuchProfanityException;
import com.intita.wschat.models.ChatUser;
import com.intita.wschat.models.ChatUserLastRoomDate;
import com.intita.wschat.models.ConfigParam;
import com.intita.wschat.models.Lang;
import com.intita.wschat.models.OperationStatus;
import com.intita.wschat.models.OperationStatus.OperationType;
import com.intita.wschat.models.Room;
import com.intita.wschat.models.User;
import com.intita.wschat.models.UserMessage;
import com.intita.wschat.repositories.ChatLangRepository;
import com.intita.wschat.repositories.UserRepository;
import com.intita.wschat.services.ChatTenantService;
import com.intita.wschat.services.ChatUserLastRoomDateService;
import com.intita.wschat.services.ChatUsersService;
import com.intita.wschat.services.ConfigParamService;
import com.intita.wschat.services.RoomsService;
import com.intita.wschat.services.UserMessageService;
import com.intita.wschat.services.UsersService;
import com.intita.wschat.util.ProfanityChecker;

import jsonview.Views;

/**
 * Controller that handles WebSocket chat messages
 * 
 * @author Nicolas
 */

@Controller
public class ChatController {
	@Autowired
	ConfigParamService configParamService;
	
	@Autowired private ProfanityChecker profanityFilter;

	@Autowired private SessionProfanity profanity;

	@Autowired private ParticipantRepository participantRepository;

	@Autowired private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired private CustomAuthenticationProvider authenticationProvider;

	@Autowired private RoomsService roomService;
	@Autowired private UsersService userService;
	@Autowired private UserMessageService userMessageService;
	@Autowired private ChatUsersService chatUsersService;
	@Autowired private ChatTenantService ChatTenantService;
	@Autowired private ChatUserLastRoomDateService chatUserLastRoomDateService;
	@Autowired private ChatLangRepository chatLangRepository;

	private final static ObjectMapper mapper = new ObjectMapper();
	private Map<String,Map<String,Object>> langMap = new HashMap<>();

	public static final Map<String,Queue<UserMessage>> messagesBuffer = new ConcurrentHashMap<String, Queue<UserMessage>>();// key => roomId
	private final Map<String,Queue<DeferredResult<String>>> responseBodyQueue =  new ConcurrentHashMap<String,Queue<DeferredResult<String>>>();// key => roomId

	public static ConcurrentHashMap<String, ArrayList<Object>> infoMap = new ConcurrentHashMap<>();
	//private ConcurrentLinkedMap<DeferredResult<String>> globalInfoResult = new ConcurrentLinkedQueue<>();
	ConcurrentHashMap<DeferredResult<String>,String> globalInfoResult = new ConcurrentHashMap<DeferredResult<String>,String>();
	public static void addFieldToInfoMap(String key, Object value)
	{
		ArrayList<Object> listElm = infoMap.get(key);
		if(listElm == null)
		{
			listElm = new ArrayList<>();
			infoMap.put(key, listElm);
		}
		listElm.add(value);
	}
	
	public static class CurrentStatusUserRoomStruct{
		private Room room;
		private ChatUser user;
		
		public Room getRoom() {
			return room;
		}

		public ChatUser getUser() {
			return user;
		}

		public CurrentStatusUserRoomStruct(ChatUser user, Room room) {
			this.room = room;
			this.user = user;
		}
	}
	
	public static CurrentStatusUserRoomStruct isMyRoom(String roomId, Principal principal, ChatUsersService chat_user_service, RoomsService chat_room_service)
	{
		Room o_room = chat_room_service.getRoom(Long.parseLong(roomId));
		if(o_room == null)
			return null;
		
		ChatUser o_user = chat_user_service.getChatUser(principal);
		if(o_user == null)
			return null;
			
		Set<Room> all = o_user.getRoomsFromUsers();
		all.addAll(o_user.getRootRooms());
		
		if(!all.contains(o_room))
			return null;
		
		return new CurrentStatusUserRoomStruct(o_user, o_room);
	}
	

	//[TIMEOUTS]
	/*@Value("${timeouts.message}")
	private final Long timeOutMessage;
	 */

	@SuppressWarnings("unchecked")
	@PostConstruct
	public void onCreate()
	{
		Iterable<Lang> it = chatLangRepository.findAll();
		for(Lang lg:it)
		{
			HashMap<String, Object> result = null;
			JsonFactory factory = new JsonFactory(); 
			ObjectMapper mapper = new ObjectMapper(factory); 
			mapper.configure(Feature.AUTO_CLOSE_SOURCE, true);

			TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};

			try {
				result = mapper.readValue(lg.getMap(), typeRef);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("Lang " + lg.getLang() + " is wrong!!!");
				e.printStackTrace();
			}
			langMap.put(lg.getLang(), result);
		}
	}
	/********************
	 * GET CHAT USERS LIST FOR TEST
	 *******************/
	@RequestMapping(value = "/chat/users", method = RequestMethod.POST)
	@ResponseBody
	public String getUsers(Principal principal) throws JsonProcessingException {

		Page<User> pageUsers = userService.getUsers(1, 15);
		Set<LoginEvent> userList = new HashSet<>();
		for(User user : pageUsers)
		{
			userList.add(new LoginEvent(user.getId(),user.getUsername(), user.getAvatar()));
		}
		return  new ObjectMapper().writeValueAsString(userList);
	}

	public UserMessage filterMessage( String roomStr,  ChatMessage message, Principal principal) {
		ChatUser chatUser = new ChatUser(Long.parseLong(principal.getName()));
		chatUser.setNickName(message.getUsername());
		Room room = new Room(Long.parseLong(roomStr));
		UserMessage messageToSave = new UserMessage(chatUser,room,message.getMessage());
		//if (messageToSave.getRoom().getName()==null || messageToSave.getBody() == null) return null;
		//message.setUsername(chatUser.getNickName());
		return messageToSave;

	}
	@MessageMapping("/{room}/chat.message")
	public ChatMessage filterMessageWS(@DestinationVariable("room") String roomStr, @Payload ChatMessage message, Principal principal) {
		//System.out.println("ZIGZAG ZIGZAG ZIGZAG ZIGZAG ZIGZAG ZIGZAG ZIGZAG ZIGZAG ZIGZAG");
		//checkProfanityAndSanitize(message);//@NEED WEBSOCKET@

		UserMessage messageToSave = filterMessage(roomStr, message, principal);
		if (messageToSave!=null)
		addMessageToBuffer(roomStr, messageToSave);
		OperationStatus operationStatus = new OperationStatus(OperationType.SEND_MESSAGE_TO_ALL,true,"SENDING MESSAGE TO ALL USERS");
		String subscriptionStr = "/topic/users/" + principal.getName() + "/status";
		simpMessagingTemplate.convertAndSend(subscriptionStr, operationStatus);
		return message;
	}

	public void addMessageToBuffer(String room, UserMessage message)
	{
		Queue<UserMessage> list = messagesBuffer.get(room);
		if(list == null)
		{
			list = new ConcurrentLinkedQueue<>();
			messagesBuffer.put(room, list);
		}
		list.add(message);

		//send message to WS users
		simpMessagingTemplate.convertAndSend("/topic/users/must/get.room.num/chat.message", room);
		ChatController.addFieldToInfoMap("newMessage", room);
	}

	@RequestMapping(value = "/{room}/chat/message", method = RequestMethod.POST)
	@ResponseBody
	public void filterMessageLP(@PathVariable("room") String roomStr,@RequestBody ChatMessage message, Principal principal) {
		//checkProfanityAndSanitize(message);//@NEED WEBSOCKET@
		UserMessage messageToSave = filterMessage(roomStr, message, principal);
		if (messageToSave!=null)
		addMessageToBuffer(roomStr, messageToSave);
		simpMessagingTemplate.convertAndSend(("/topic/" + roomStr + "/chat.message"), message);
	}

	@RequestMapping(value = "/{room}/chat/message/update", method = RequestMethod.POST)
	@ResponseBody
	public DeferredResult<String> updateMessageLP(@PathVariable("room") String room) throws JsonProcessingException {
		Long timeOut = 1000000000L;
		DeferredResult<String> result = new DeferredResult<String>(timeOut, "{}");
		Queue<DeferredResult<String>> queue = responseBodyQueue.get(room);
		if(queue == null)
		{
			queue = new ConcurrentLinkedQueue<DeferredResult<String>>();
		}
		responseBodyQueue.put(room, queue);
		//System.out.println("updateMessageLP responseBodyQueue:"+queue.size());
		queue.add(result);
		return result;
	}

	@Scheduled(fixedDelay=600L)
	public void processMessage(){

		for(String roomId : messagesBuffer.keySet())
		{
			Queue<UserMessage> array = messagesBuffer.get(roomId);
			Queue<DeferredResult<String>> responseList = responseBodyQueue.get(roomId);
			if(responseList != null)
			{
				for(DeferredResult<String> response : responseList)
				{
					String str = "";
					if(responseList != null)
					{
						try {
							str = mapper.writeValueAsString(ChatMessage.getAllfromUserMessages(array));
						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}												
					}
					if(!response.isSetOrExpired())
						response.setResult(str);
				}
				responseList.clear();
				//System.out.println("processMessage responseBodyQueue:"+responseList.size());
			}

			userMessageService.addMessages(array);
			messagesBuffer.remove(roomId);
		}
	}

	@RequestMapping(value = "/chat/global/lp/info", method = RequestMethod.POST)
	@ResponseBody
	public DeferredResult<String> updateGlobalInfoLP(Principal principal) throws JsonProcessingException {

		Long timeOut = 10000L;
		participantRepository.add(principal.getName());
		DeferredResult<String> result = new DeferredResult<String>(timeOut, "{}");
		globalInfoResult.put(result,principal.getName());
		
		LoginEvent loginEvent = new LoginEvent(Long.parseLong(principal.getName()), "test");
		simpMessagingTemplate.convertAndSend("/topic/chat.login", loginEvent);
		
		System.out.println("globalInfoResult.add:"+principal.getName());
		return result;
	}

	@Scheduled(fixedDelay=5000L)
	public void processGlobalInfo(){
		String result;
		try {
			result = mapper.writeValueAsString(infoMap);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = "{}";
		}
		infoMap.clear();
		for(DeferredResult<String> nextUser : globalInfoResult.keySet())
		{
		
		//	System.out.println("globalInfoResult.remove:"+globalInfoResult.get(nextUser));
			participantRepository.getActiveSessions().remove(globalInfoResult.get(nextUser));
			
			LoginEvent loginEvent = new LoginEvent(Long.parseLong(globalInfoResult.get(nextUser)), globalInfoResult.get(nextUser));
			simpMessagingTemplate.convertAndSend("/topic/chat.logout", loginEvent);
			
			if(!nextUser.isSetOrExpired())
				nextUser.setResult(result);		
		}
		globalInfoResult.clear();

	}
	//NOT TEST!!!
	@MessageMapping("/{room}/chat.private.{username}")
	public void filterPrivateMessage(@DestinationVariable String room,@Payload ChatMessage message, @DestinationVariable("username") String username, Principal principal) {
		checkProfanityAndSanitize(message);
		Long chatUserId = 0L;
		chatUserId = Long.parseLong(principal.getName());
		message.setUsername(principal.getName());
		OperationStatus operationStatus = new OperationStatus(OperationType.SEND_MESSAGE_TO_USER,true,"SENDING MESSAGE TO USER");
		String subscriptionStr = "/topic/users/"+chatUserId+"/status";
		simpMessagingTemplate.convertAndSend(subscriptionStr, operationStatus);

		simpMessagingTemplate.convertAndSend("/user/" + username + "/exchange/amq.direct/"+room+"/chat.message", message);
	}


	/*
	 * Go into room
	 */
	@MessageMapping("/chat.go.to.dialog/{roomId}")
	public void userGoToDialogListener(@DestinationVariable("roomId") String roomid, Principal principal) {
		//	checkProfanityAndSanitize(message);

		Long user_id = Long.parseLong(principal.getName(), 10);
		Long room_id = Long.parseLong(roomid, 10);
		CurrentStatusUserRoomStruct struct =  isMyRoom(roomid, principal, chatUsersService, roomService);
		if(struct == null)
			return;
		
		ChatUser user = struct.getUser();
		Room room = struct.getRoom();
		ChatUserLastRoomDate last = chatUserLastRoomDateService.getUserLastRoomDate(room , user);
		last.setLastLogout(new Date());
		chatUserLastRoomDateService.updateUserLastRoomDateInfo(last);
	}
	@RequestMapping(value = "/chat.go.to.dialog/{roomId}", method = RequestMethod.POST)
	@ResponseBody
	public void userGoToDialogListenerLP(@PathVariable("roomId") String roomid, Principal principal) {
		userGoToDialogListener(roomid, principal);
	}

	/*
	 * Out from room
	 */
	@MessageMapping("/chat.go.to.dialog.list/{roomId}")
	public void userGoToDialogListListener(@DestinationVariable("roomId") String roomid, Principal principal) {
		//	checkProfanityAndSanitize(message);

		Long user_id = Long.parseLong(principal.getName(), 10);
		Long room_id = Long.parseLong(roomid, 10);
		CurrentStatusUserRoomStruct struct =  isMyRoom(roomid, principal, chatUsersService, roomService);
		if(struct == null)
			return;
		
		ChatUser user = struct.getUser();
		Room room = struct.getRoom();
		
		if (room == null)
			return;
		//public ChatUserLastRoomDate(Long id, Date last_logout, Long last_room){
		ChatUserLastRoomDate last = chatUserLastRoomDateService.getUserLastRoomDate( room, user);
		if (last == null)
		{
			last = new ChatUserLastRoomDate(user_id, new Date(),room );
			last.setChatUser(user);
		}
		else
		{
			last.setLastLogout(new Date());
		}		
		chatUserLastRoomDateService.updateUserLastRoomDateInfo(last);
	}
	@RequestMapping(value = "/chat.go.to.dialog.list/{roomId}", method = RequestMethod.POST)
	@ResponseBody
	public void userGoToDialogListListenerLP(@PathVariable("roomId") String roomid, Principal principal) {

		userGoToDialogListListener(roomid, principal);
	}

	/* 
	 *Work only on WS
	 */
	private void checkProfanityAndSanitize(ChatMessage message) {
		long profanityLevel = profanityFilter.getMessageProfanity(message.getMessage());
		profanity.increment(profanityLevel);
		message.setMessage(profanityFilter.filter(message.getMessage()));
	}

	/*@RequestMapping(value="/getusersemails", method = RequestMethod.POST)
	@ResponseBody
	public String getEmails(@RequestParam String login) throws JsonProcessingException {
		List<String> emails = userService.getUsersEmailsFist5(login);
		ObjectMapper mapper = new ObjectMapper();
		String jsonInString = mapper.writeValueAsString(emails);
		return jsonInString;
	}*/

	@RequestMapping(value="/get_users_emails_like", method = RequestMethod.GET)
	@ResponseBody
	public String getEmailsLike(@RequestParam String login, @RequestParam Long room) throws JsonProcessingException {

		Set<ChatUser>  users_set = roomService.getRoom(room).getUsers();
		List<ChatUser> users = new  ArrayList<ChatUser>();
		users.addAll(users_set);

		users.add(roomService.getRoom(room).getAuthor());
		List<String> room_emails = new  ArrayList<String>();
		for(int i = 0; i <  users.size(); i++)
		{
			room_emails.add(users.get(i).getNickName());
		}

		List<String> emails = userService.getUsersEmailsFist5(login, room_emails);

		ObjectMapper mapper = new ObjectMapper();

		String jsonInString = mapper.writeValueAsString(emails);
		return jsonInString;
	}

	@RequestMapping(value="/get_users_nicknames_like", method = RequestMethod.GET)
	@ResponseBody
	public Set<LoginEvent>  getNickNamesLike(@RequestParam String nickName, @RequestParam Long room) throws JsonProcessingException {

		Set<ChatUser>  users_set = roomService.getRoom(room).getChatUsers();
		List<ChatUser> users = new  ArrayList<ChatUser>();
		users.addAll(users_set);

		users.add(roomService.getRoom(room).getAuthor());
		Set<LoginEvent> usersData = new HashSet<LoginEvent>();

		List<String> room_nicks = new  ArrayList<String>();
		for(int i = 0; i <  users.size(); i++)
		{
			room_nicks.add(users.get(i).getNickName());
		}

		List<String> nicks = chatUsersService.getUsersNickNameFist5(nickName, room_nicks);

		for (ChatUser singleChatUser: users){
			String nn = singleChatUser.getNickName();
			if (!nicks.contains(nn))continue;
			LoginEvent userData = new LoginEvent(singleChatUser.getId(),nn);
			usersData.add(userData);	
		}
		return usersData;
		/*ObjectMapper mapper = new ObjectMapper();

					String jsonInString = mapper.writeValueAsString(nicks);
					return jsonInString;*/
	}

	public void addLocolization(Model model)
	{
		ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		HttpSession session = attr.getRequest().getSession(false);
		String lg;
		if(session != null)
			lg = (String) session.getAttribute("chatLg");
		else
			lg = "en";

		model.addAttribute("lgPack", langMap.get(lg));
	}
	@RequestMapping(value="/", method = RequestMethod.GET)
	public String  getIndex(HttpRequest request, Model model) {
		authenticationProvider.autorization(authenticationProvider);
		addLocolization(model);
		return "index";
	}
	@RequestMapping(value="/{page}.html", method = RequestMethod.GET)
	public String  getTeachersTemplate(HttpRequest request, @PathVariable("page") String page, Model model) {
		//HashMap<String,Object> result =   new ObjectMapper().readValue(JSON_SOURCE, HashMap.class);
		List<ConfigParam> config =  configParamService.getParams();

		addLocolization(model);
		model.addAttribute("config",ConfigParam.listAsMap(config));
		return page;
	}

	@MessageExceptionHandler
	@SendToUser(value = "/exchange/amq.direct/errors", broadcast = false)
	public String handleProfanity(TooMuchProfanityException e) {
		return e.getMessage();
	}
	
	@RequestMapping(value="/get_room_messages", method = RequestMethod.GET)
	@ResponseBody
	public String  getRoomMessages(@RequestParam Long roomId, Principal principal) throws JsonProcessingException {
		    mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		    boolean isAdmin = userService.isAdmin(principal.getName());
		    if(!isAdmin)
		    	return null;
	return mapper.writerWithView(Views.Public.class).writeValueAsString(userMessageService.getUserMessagesByRoomId(roomId));
	}
	
	
}