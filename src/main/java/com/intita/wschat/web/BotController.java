package com.intita.wschat.web;

import java.security.Principal;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intita.wschat.models.BotItemContainer;
import com.intita.wschat.models.BotSequence;
import com.intita.wschat.services.BotSequenceService;

@Service
@Controller
public class BotController {
	@Autowired
	BotSequenceService botSeuenceService;
	
	@PostConstruct
	public void addTestInfoToDb(){
		botSequence = generateTestSequnce();
		botSeuenceService.add(botSequence);
	}
	
	BotItemContainer currentContainer = null;
	BotSequence botSequence;
	
	public String getJsonContainerBodySimple(String[] itemsText){
		String res = "{";
		for (int i = 0 ; i < itemsText.length;i++){
			if(i!=0)res += ",";
			res += String.format("item%d:{'data':'%s'}",i,itemsText[i]);
		}
		res+="}";
		return res;
	}
	public BotSequence generateTestSequnce(){
		BotSequence botSequence = new BotSequence();
		String[] container1 = {"Variant1,Variant2,Variant3,Variant4"};
		BotItemContainer testItemContainer1 = new BotItemContainer(getJsonContainerBodySimple(container1));//begin
		BotItemContainer testItemContainer2 = new BotItemContainer(getJsonContainerBodySimple(container1));
		BotItemContainer testItemContainer3 = new BotItemContainer(getJsonContainerBodySimple(container1));
		BotItemContainer testItemContainer4 = new BotItemContainer(getJsonContainerBodySimple(container1));//end
		BotItemContainer testItemContainer5 = new BotItemContainer(getJsonContainerBodySimple(container1));
		testItemContainer1.addBranch(0, testItemContainer2);
		testItemContainer2.addBranch(0, testItemContainer3);
		testItemContainer3.addBranch(0, testItemContainer4);
		botSequence.addElement(testItemContainer1);
		return botSequence;
	}
	
	@RequestMapping(value = "bot_operations/sequences/{sequenceId}/{containerId}/nextContainer{choseIndex}", method = RequestMethod.GET)
	@ResponseBody
	public String getSequence(@PathVariable Long sequenceId,@PathVariable Long containerId, @PathVariable int choseIndex) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		
		 
		return objectMapper.writeValueAsString(botSequence);
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
