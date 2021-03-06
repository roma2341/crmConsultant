package com.intita.wschat.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intita.wschat.models.Lang;
import com.intita.wschat.repositories.ChatLangRepository;
import com.intita.wschat.web.ChatController;

@Service
@Transactional
public class ChatLangService {
	
	private final static Logger log = LoggerFactory.getLogger(ChatController.class);
	@Autowired
private ChatLangRepository chatLangRepository;
	private Map<String,Map<String,Object>> localizationMap = new HashMap<>();

public  Map<String,Map<String,Object>> updateDataFromDatabase()
{
	 Map<String,Map<String,Object>> langMap = new HashMap<>();
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
		log.info("Current lang pack" + langMap.toString());

	}
	localizationMap = langMap;
	return langMap;
}
public Map<String,Map<String,Object>> getLocalizationMap(){
	return localizationMap;
}
public Map<String, Object> getLocalization()
{
	
	return (Map<String, Object>) getLocalizationMap().get(ChatController.getCurrentLang());
}
@PostConstruct
private void initService(){
	
}
}
