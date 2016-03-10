package com.intita.wschat.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Queue;

import com.intita.wschat.models.UserMessage;

/**
 * 
 * @author Sergi Almar
 */
public class ChatMessage {

	private String username;
	private String message;
	private Long chatUserId;
	private Date date;
	private ArrayList<String> attachedFiles = new ArrayList<String>();
	public ChatMessage(){
		date = new Date();
	}
	public ChatMessage(UserMessage usrMsg){
		this.username = usrMsg.getAuthor().getNickName();
		this.message = usrMsg.getBody();
		this.attachedFiles = usrMsg.getAttachedFiles();
		date = new Date();
	}
	
	static public ArrayList<ChatMessage> getAllfromUserMessages (Iterable<UserMessage> userMessages){
		ArrayList<ChatMessage> result = new ArrayList<ChatMessage>();
		for(UserMessage singleUserMessage : userMessages){
			result.add(new ChatMessage(singleUserMessage));
		}
		return result;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	@Override
	public String toString() {
		return "ChatMessage ";
	}
	public Long getChatUserId() {
		return chatUserId;
	}
	public void setChatUserId(Long chatUserId) {
		this.chatUserId = chatUserId;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public ArrayList<String> getAttachedFiles() {
		return attachedFiles;
	}
	public void setAttachedFiles(ArrayList<String> attachedFiles) {
		this.attachedFiles = attachedFiles;
	}
	
}
