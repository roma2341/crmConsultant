package com.intita.wschat.web;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intita.wschat.domain.ChatMessage;
import com.intita.wschat.models.BotAnswer;
import com.intita.wschat.models.BotCategory;
import com.intita.wschat.models.BotDialogItem;
import com.intita.wschat.models.ChatTenant;
import com.intita.wschat.models.ChatUser;
import com.intita.wschat.models.LangId;
import com.intita.wschat.models.Room;
import com.intita.wschat.models.UserMessage;
import com.intita.wschat.repositories.ChatLangRepository;
import com.intita.wschat.services.BotAnswersService;
import com.intita.wschat.services.BotCategoryService;
import com.intita.wschat.services.BotItemContainerService;
import com.intita.wschat.services.ChatTenantService;
import com.intita.wschat.services.ChatUserLastRoomDateService;
import com.intita.wschat.services.ChatUsersService;
import com.intita.wschat.services.ConsultationsService;
import com.intita.wschat.services.CourseService;
import com.intita.wschat.services.RoomsService;
import com.intita.wschat.services.UserMessageService;
import com.intita.wschat.services.UsersService;
import com.intita.wschat.web.ChatController.ChatLangEnum;

@Service
@Controller
public class BotController {
	@Autowired
	BotCategoryService botCategoryService;

	@Autowired
	BotItemContainerService botItemContainerService;

	@Autowired private SimpMessagingTemplate simpMessagingTemplate;

	@Autowired private RoomsService roomService;
	@Autowired private UsersService userService;
	@Autowired private UserMessageService userMessageService;
	@Autowired private ChatUsersService chatUsersService;
	@Autowired private ChatTenantService chatTenantService;
	@Autowired private ChatUserLastRoomDateService chatUserLastRoomDateService;
	@Autowired private ChatLangRepository chatLangRepository;
	@Autowired private ConsultationsService chatConsultationsService;
	@Autowired private CourseService courseService;
	@Autowired private BotAnswersService botAnswerService;



	@Autowired private RoomController roomControler;
	@Autowired private ChatController chatController;

	private ObjectMapper objectMapper = new ObjectMapper();
	private final static Logger log = LoggerFactory.getLogger(BotController.class);

	@PostConstruct
	public void postConstructor(){

		ChatUser chatUser = new ChatUser("Mr.Bot", null);
		chatUser.setId((long)0);
		chatUsersService.updateChatUserInfo(chatUser);

		if (botCategoryService.getCount() > 0)
			return;

		botCategory = botCategoryService.add(new BotCategory("Main category"));
		generateTestSequnce(botCategory);
	}	

	BotDialogItem currentContainer = null;
	BotCategory botCategory;

	public String getJsonContainerBodySimple(String[] itemsText){
		String res = "{";
		for (int i = 0 ; i < itemsText.length;i++){
			if(i!=0)res += ",";
			res += String.format("item%d:{'data':'%s'}",i,itemsText[i]);
		}
		res+="}";
		return res;
	}
	public void generateTestSequnce(BotCategory botCategory){
		String[] container1 = {"Variant1,Variant2,Variant3,Variant4"};
		BotDialogItem testItemContainer1 = botItemContainerService.add(new BotDialogItem(getJsonContainerBodySimple(container1),botCategory,1L,"ua"));//begin
		BotDialogItem testItemContainer2 = botItemContainerService.add(new BotDialogItem(getJsonContainerBodySimple(container1),botCategory,2L,"ua"));
		BotDialogItem testItemContainer3 = botItemContainerService.add(new BotDialogItem(getJsonContainerBodySimple(container1),botCategory,3L,"ua"));
		BotDialogItem testItemContainer4 = botItemContainerService.add(new BotDialogItem(getJsonContainerBodySimple(container1),botCategory,4L,"ua"));//end
		botItemContainerService.update(testItemContainer1);
		botItemContainerService.update(testItemContainer2);
		botItemContainerService.update(testItemContainer3);
		botItemContainerService.update(testItemContainer4);
		botCategory.setMainElement(testItemContainer1);
		botCategoryService.update(botCategory);
		botItemContainerService.update(testItemContainer1);
	}

	@RequestMapping(value = "bot_operations/sequences/{sequenceId}/{containerId}/nextContainer{choseIndex}", method = RequestMethod.GET)
	@ResponseBody
	public String getSequence(@PathVariable Long sequenceId,@PathVariable Long containerId, @PathVariable int choseIndex) throws JsonProcessingException {
		return objectMapper.writeValueAsString(botCategory);
	}
	@RequestMapping(value = "bot_operations/get_bot_dialog_item/{dialogItemId}", method = RequestMethod.GET)
	@ResponseBody
	public String getBotDialogItem(@PathVariable Long dialogItemId,  HttpServletRequest request, Principal principal) throws JsonProcessingException {
		Map<String, BotDialogItem> array = new HashMap<>();
		for(String lang : ChatLangEnum.LANGS)
		{
			BotDialogItem dialogItemTemplate = botItemContainerService.getByIdAndLang(dialogItemId, lang);
			array.put(lang, dialogItemTemplate);
		}
		return objectMapper.writeValueAsString(array);
	}

	@RequestMapping(value = "bot_operations/create_bot_dialog_item", method = RequestMethod.POST)
	@ResponseBody
	public String addBotDialogItem( HttpServletRequest request, HttpServletResponse response, Principal principal,@RequestBody BotDialogItem payload) throws JsonProcessingException {
		if(payload == null)
		{
			log.error("send params (object) faild!");//@LANG@
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return "Send params (object) faild!";
		}
		
		BotCategory category = payload.getCategory();
		if (category == null || category.getId() == null){
			log.error("Category is NULL !");
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return "Category is NULL !";//@LANG@
		}
		
		Map<String, BotDialogItem> array = new HashMap<>();
		Long nextId = botItemContainerService.getNextId();
		
		for(String lang : ChatLangEnum.LANGS)
		{
			BotDialogItem dialogItemTemplate = new BotDialogItem(payload);
			dialogItemTemplate.setIdObject(new LangId(nextId, lang));
			botItemContainerService.add(dialogItemTemplate);
			array.put(lang, dialogItemTemplate);
		}
		return objectMapper.writeValueAsString(array);
	}

	@RequestMapping(value = "bot_operations/save_dialog_item", method = RequestMethod.POST)
	@ResponseBody
	public boolean saveBotDialogItem(HttpServletRequest request,HttpServletResponse response, Principal principal,@RequestBody BotDialogItem payload) throws JsonProcessingException {
		botItemContainerService.update(payload);
		return true;
	}

	@RequestMapping(value = "bot_operations/{roomId}/submit_dialog_item/{containerId}/next_item/{nextContainerId}", method = RequestMethod.POST)
	@ResponseBody
	public String getSequence(@PathVariable Long roomId,@PathVariable Long containerId, @PathVariable Long nextContainerId, HttpServletRequest request, Principal principal) throws JsonProcessingException {
		/*Iterator it = params.entrySet().iterator();
		while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        log.info("map submitted:");
	        log.info( (pair.getKey() + " = " + pair.getValue() ) );
	        it.remove(); // avoids a ConcurrentModificationException
	    }*/
		String jsonBody="";
		try {
			jsonBody = IOUtils.toString( request.getInputStream(),"UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("jsonbody:"+jsonBody);
		HashMap<String,String> obj =null;
		try {
			obj = new ObjectMapper().readValue(jsonBody,new TypeReference<Map<String, String>>(){});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<String> keys = new ArrayList<String>(obj.keySet());

		Room room = roomService.getRoom(roomId);
		BotDialogItem item = botItemContainerService.getByObjectId(new LangId(containerId,ChatController.getCurrentLang()));

		for(int i = 0; i < keys.size(); i++)
		{
			botAnswerService.add(new BotAnswer(keys.get(i), item, room, obj.get(keys.get(i))));
		}
		Map<String,Object> param = new HashMap<String, Object>();
		param.put("nextNode", nextContainerId.toString());//@BAD

		sendNextContainer(roomId, containerId, param, principal);

		return "good";
	}

	private static Object cloneObject(Object obj){
		try{
			Object clone = obj.getClass().newInstance();
			for (Field field : obj.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				field.set(clone, field.get(obj));
			}
			return clone;
		}catch(Exception e){
			return null;
		}
	}

	@RequestMapping(value = "bot_operations/{roomId}/get_bot_container/{containerId}", method = RequestMethod.POST)
	@ResponseBody
	public String sendNextContainer(@PathVariable("roomId") Long roomId, @PathVariable("containerId") Long containerId, @RequestBody Map<String,Object> param, Principal principal) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();

		ChatUser bot = chatUsersService.getChatUser(BotParam.BOT_ID);

		Room room = roomService.getRoom(roomId);

		/*OperationStatus operationStatus = new OperationStatus(OperationType.ADD_ROOM_ON_LOGIN,true,""+room.getId());
		String subscriptionStr = "/topic/users/" + bot.getId() + "/status";
		simpMessagingTemplate.convertAndSend(subscriptionStr, operationStatus);*/
		boolean toMainContainer = param.get("category") == null;
		BotDialogItem nextContainer;
		if(!param.containsKey("nextNode"))
			return  "";

		BotDialogItem nextContainerToSave = null;
		Long nextNode = Long.parseLong((String) param.get("nextNode"));

		if(toMainContainer) {
			nextContainer = botItemContainerService.getByObjectId(new LangId(nextNode,ChatController.getCurrentLang()));	
		}
		else {
			nextContainer = botItemContainerService.getByObjectId(new LangId(nextNode,ChatController.getCurrentLang()));	
		}

		/*nextContainerToSave = new BotDialogItem(nextContainer);
		nextContainerToSave.setBody(nextContainer.getIdObject().getId().toString());
*/
		String containerString = "";
		String containerStringToSave = "";
		try {
			containerString = objectMapper.writeValueAsString(nextContainer);
			containerStringToSave = objectMapper.writeValueAsString(nextContainer.getIdObject());
				
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		UserMessage msg = new UserMessage(chatUsersService.getChatUser(principal), room, "You answer: " + nextContainer.getIdObject().getId());
		chatController.filterMessageLP(room.getId(), new ChatMessage(msg), principal);

		UserMessage qmsg = new UserMessage(bot, room, containerString);
		UserMessage messageToSave = new UserMessage(bot, room, containerStringToSave);

		chatController.filterMessageBot(room.getId(), new ChatMessage(qmsg), messageToSave);
		return objectMapper.writeValueAsString(botCategory);
	}


	@RequestMapping(value = "bot_operations/close/roomId/{roomId}", method = RequestMethod.POST)
	@ResponseBody
	public void giveTenant(@PathVariable Long roomId) throws JsonProcessingException {

		Room room_o = roomService.getRoom(roomId);
		if(room_o == null)
			return;			

		ArrayList<ChatTenant> countTenant = chatTenantService.getTenants();
		if(countTenant.isEmpty())
		{
			return;
		}
		int k = new Random().nextInt(countTenant.size());

		ChatTenant t_user = countTenant.get(k);//choose method
		ChatUser c_user = t_user.getChatUser();
		ChatUser b_user = chatUsersService.getChatUser(BotController.BotParam.BOT_ID);

		/*
		 * Tenant author of room && can add new user
		 */

		room_o.setAuthor(c_user);
		roomService.update(room_o);

		roomControler.addUserToRoom(b_user, room_o, b_user.getPrincipal(), true);


	}
	
	@RequestMapping(value = "bot_operations/get_all_categories_ids", method = RequestMethod.GET)
	@ResponseBody
	public ArrayList<Long> getAllCategoriesIds() throws JsonProcessingException {
		ArrayList<Long> categories = botCategoryService.getAllIds();
		return categories;

	}
	@RequestMapping(value = "bot_operations/get_five_categories_names_like/{categoryName}", method = RequestMethod.GET)
	@ResponseBody
	public ArrayList<String> get5CategoriesNamesLike(@PathVariable String categoryName) throws JsonProcessingException {
		ArrayList<String> categories = botCategoryService.getFirst5NamesLike(categoryName);
		return categories;

	}
	
	@RequestMapping(value = "bot_operations/get_categories_ids_where_names_like/{categoryName}", method = RequestMethod.GET)
	@ResponseBody
	public ArrayList<Long> getCategoriesIdsWhereNameLike(@PathVariable String categoryName) throws JsonProcessingException {
		ArrayList<Long> categories = botCategoryService.getIdsWhereNamesLike(categoryName);
		return categories;

	}
	///////
	@RequestMapping(value = "bot_operations/get_five_dialog_items_description_where_description_like/{categoryId}/{description}", method = RequestMethod.GET)
	@ResponseBody
	public ArrayList<String> get5DialogItemsDescriptionWhereDescriptionLike(@PathVariable Long categoryId,@PathVariable String description) throws JsonProcessingException {
		ArrayList<String> categories = null;
		if (categoryId==null)categories = botItemContainerService.getFirst5DescriptionsLike(description);
		else
		categories = botItemContainerService.getFirst5DescriptionsLike(description,categoryId);
		return categories;

	}
	
	@RequestMapping(value = "bot_operations/get_dialog_items_ids_where_description_like/{categoryId}/{description}", method = RequestMethod.GET)
	@ResponseBody
	public String getDialogItemsIdsWhereDescriptionLike(@PathVariable Long categoryId,@PathVariable String description) throws JsonProcessingException {
		ArrayList<Long> dialogItems = null;
		if (categoryId==null)
			dialogItems = botItemContainerService.getIdsWhereDescriptionsLike(description);
		else
			dialogItems = botItemContainerService.getIdsWhereDescriptionsLike(description,categoryId);
		return objectMapper.writeValueAsString(dialogItems);

	}
/////////
	@RequestMapping(value = "bot_operations/get_all_dialog_items_ids", method = RequestMethod.GET)
	@ResponseBody
	public ArrayList<Long> getAllDialogItemsIds() throws JsonProcessingException {
		ArrayList<Long> dialogItems = botItemContainerService.getAllIds();
		return dialogItems;

	}
	@RequestMapping(value = "bot_operations/get_dialog_items_ids/{categoryId}", method = RequestMethod.GET)
	@ResponseBody
	public ArrayList<Long> getDialogItemsIds(@PathVariable Long categoryId) throws JsonProcessingException {
		ArrayList<Long> dialogItems = botItemContainerService.getAllIdsFromCategory(categoryId);
		return dialogItems;

	}

	public static class BotParam{
		public static final long BOT_ID = 0;
		public static final String BOT_AVATAR = "noname.png";
		public static Principal getBotPrincipal()
		{
			return new Principal() {

				@Override
				public String getName() {
					return String.valueOf(BOT_ID);
				}
			};

		}
	}
}
