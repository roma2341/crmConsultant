package com.intita.wschat.repositories;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import com.intita.wschat.models.ChatUser;
import com.intita.wschat.models.Room;
import com.intita.wschat.models.User;
import com.intita.wschat.models.UserMessage;

@Qualifier("IntitaConf") 
public interface UserMessageRepository  extends CrudRepository<UserMessage, Long>{
	  Page<UserMessage> findById(Long id, Pageable pageable);
	  ArrayList<UserMessage> findAll(Pageable pageable);
	  ArrayList<UserMessage> findByAuthor(ChatUser author);
	  ArrayList<UserMessage> findByRoom(Room room);
}