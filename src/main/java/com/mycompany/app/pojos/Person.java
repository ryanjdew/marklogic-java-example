package com.mycompany.app.pojos;

import  java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.marklogic.client.pojo.annotation.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("person")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Person { 
  @JsonProperty("_id")
  @Id
  public String _id;
  @JsonProperty("index")
  public Double index;
  @JsonProperty("guid")
  public String guid;
  @JsonProperty("isActive")
  public Boolean isActive;
  @JsonProperty("balance")
  public String balance;
  @JsonProperty("picture")
  public String picture;
  @JsonProperty("age")
  public Double age;
  @JsonProperty("eyeColor")
  public String eyeColor;
  @JsonProperty("name")
  public String name;
  @JsonProperty("gender")
  public String gender;
  @JsonProperty("company")
  public String company;
  @JsonProperty("email")
  public String email;
  @JsonProperty("phone")
  public String phone;
  @JsonProperty("address")
  public String address;
  @JsonProperty("about")
  public String about;
  @JsonProperty("registered")
  public String registered;
  @JsonProperty("latitude")
  public Double latitude;
  @JsonProperty("longitude")
  public Double longitude;
  @JsonProperty("tags")
  public List<String> tags;
  @JsonProperty("greeting")
  public String greeting;
  @JsonProperty("favoriteFruit")
  public String favoriteFruit;

  public String get_id() { 
    return this._id; 
  }

  public void set_id(String _id) { 
    this._id = _id; 
  }


  public Double getIndex() { 
    return this.index; 
  }

  public void setIndex(Double index) { 
    this.index = index; 
  }


  public String getGuid() { 
    return this.guid; 
  }

  public void setGuid(String guid) { 
    this.guid = guid; 
  }


  public Boolean getIsActive() { 
    return this.isActive; 
  }

  public void setIsActive(Boolean isActive) { 
    this.isActive = isActive; 
  }


  public String getBalance() { 
    return this.balance; 
  }

  public void setBalance(String balance) { 
    this.balance = balance; 
  }


  public String getPicture() { 
    return this.picture; 
  }

  public void setPicture(String picture) { 
    this.picture = picture; 
  }


  public Double getAge() { 
    return this.age; 
  }

  public void setAge(Double age) { 
    this.age = age; 
  }


  public String getEyeColor() { 
    return this.eyeColor; 
  }

  public void setEyeColor(String eyeColor) { 
    this.eyeColor = eyeColor; 
  }


  public String getName() { 
    return this.name; 
  }

  public void setName(String name) { 
    this.name = name; 
  }


  public String getGender() { 
    return this.gender; 
  }

  public void setGender(String gender) { 
    this.gender = gender; 
  }


  public String getCompany() { 
    return this.company; 
  }

  public void setCompany(String company) { 
    this.company = company; 
  }


  public String getEmail() { 
    return this.email; 
  }

  public void setEmail(String email) { 
    this.email = email; 
  }


  public String getPhone() { 
    return this.phone; 
  }

  public void setPhone(String phone) { 
    this.phone = phone; 
  }


  public String getAddress() { 
    return this.address; 
  }

  public void setAddress(String address) { 
    this.address = address; 
  }


  public String getAbout() { 
    return this.about; 
  }

  public void setAbout(String about) { 
    this.about = about; 
  }


  public String getRegistered() { 
    return this.registered; 
  }

  public void setRegistered(String registered) { 
    this.registered = registered; 
  }


  public Double getLatitude() { 
    return this.latitude; 
  }

  public void setLatitude(Double latitude) { 
    this.latitude = latitude; 
  }


  public Double getLongitude() { 
    return this.longitude; 
  }

  public void setLongitude(Double longitude) { 
    this.longitude = longitude; 
  }


  public List<String> getTags() { 
    return this.tags; 
  }

  public void setTags(List<String> tags) { 
    this.tags = tags; 
  }


  public String getGreeting() { 
    return this.greeting; 
  }

  public void setGreeting(String greeting) { 
    this.greeting = greeting; 
  }


  public String getFavoriteFruit() { 
    return this.favoriteFruit; 
  }

  public void setFavoriteFruit(String favoriteFruit) { 
    this.favoriteFruit = favoriteFruit; 
  }

}