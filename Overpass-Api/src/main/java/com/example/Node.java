package com.example;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Node {
     private String id;
     private String lat;
     private String lon;
     private List<Tag> tags;

     public Node() {
          tags = new ArrayList<>();
     }

     @Override
     public String toString() {
          return " "+tags;
     }

     @Data
     public static class Tag {
          private String k;
          private String v;

          @Override
          public String toString() {
               return k + '\'' + v + '\'';
          }
     }
}


