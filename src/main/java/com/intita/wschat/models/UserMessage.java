package com.intita.wschat.models;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
public class UserMessage implements Serializable  {
	
	public UserMessage(){
		
	}
	public UserMessage(ChatUser author, Room room, String body){
	this.author = author;
	this.room = room;
	this.body = body;
	}
	
	@Id
	@GeneratedValue
	private Long id;
	
	//@NotBlank
	@ManyToOne
	@JsonManagedReference
	private ChatUser author;
	
	//@NotBlank
	@ManyToOne
	private Room room;
	
	@Size(max=64000)
	@Column
	private String body;

	public ChatUser getAuthor() {
		return author;
	}
	public void setAuthor(ChatUser author) {
		this.author = author;
	}
	public Room getRoom() {
		return room;
	}
	public void setRoom(Room room) {
		this.room = room;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	
}