package com.asiacom.mqtt;


import java.util.Date;

import android.location.Location;

//事件
public class Events {
    public static abstract class E {
        Date date;
        public E() {
            this.date = new Date();
        }
        public Date getDate() {
            return date;
        } 
       
    }
    
    public static class PublishSuccessfull extends E{
        Object extra;
        public PublishSuccessfull(Object extra) {
            this.extra = extra;
            this.date = new Date();
        }
        public Object getExtra() {
            return extra;
        }
    }
    
    //位置更新
//    public static class LocationUpdated  extends E{
//        GeocodableLocation l; 
//        
//        public LocationUpdated(GeocodableLocation l) {
//            this.l = l;
//            
//        }
//
//        public GeocodableLocation getGeocodableLocation() {
//            return l;
//        }
//        
//        
//    }
    
    public static class StateChanged {
        public static class ServiceMqtt extends E{
            private Constants.State.ServiceMqtt state;
            private Object extra;
            
            public ServiceMqtt(Constants.State.ServiceMqtt state) {
               this(state, null);
            }
            
            public ServiceMqtt(Constants.State.ServiceMqtt state, Object extra) {
                this.state = state;
                this.extra = extra;
            }
            public Constants.State.ServiceMqtt getState() {
                return this.state;
            }
            public Object getExtra() {
                return extra;
            }
            
        }
        public static class ServiceLocator  extends E {
            private Constants.State.ServiceLocator state;
            public ServiceLocator(Constants.State.ServiceLocator state) {
                this.state = state;
            }
            public Constants.State.ServiceLocator getState() {
                return this.state;
            }
            
        }
   }
}
