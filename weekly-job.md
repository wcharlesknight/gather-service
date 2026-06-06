I am currently trying to implement the main feature of this backend service which is a backend job that runs weekly and looks through all of our users in firebase and looks at their locations, this is what the user data model looks like:
```
{
  "displayName": "Iceberg",
  "email": "ice@ice.com",
  "hasCompletedOnboarding": true,
  "lastLoginAt": "2026-04-18T08:20:36-07:00",
  "location": {
    "cityId": "seattle",
    "cityName": "Seattle",
    "country": "USA",
    "latitude": 47.6062,
    "longitude": -122.3321,
    "savedAt": "2026-04-18T10:18:55-07:00",
    "state": "WA"
  }
}
```

and then will hit the places api for whatever cities it finds among users, and pick one for each city. Then it will send a push notification and an email to those users. We are currently dealing with a bug when testing this weekly job:

```
Caused by: com.google.api.gax.rpc.FailedPreconditionException: io.grpc.StatusRuntimeException: FAILED_PRECONDITION: The query requires an index. You can create it here: https://console.firebase.google.com/v1/r/project/gatherus-93c0a/firestore/indexes?create_composite=ClVwcm9qZWN0cy9nYXRoZXJ1cy05M2MwYS9kYXRhYmFzZXMvKGRlZmF1bHQpL2NvbGxlY3Rpb25Hcm91cHMvZ2F0aGVyaW5nU3BvdHMvaW5kZXhlcy9fEAEaCgoGY2l0eUlkEAEaDAoIcHJvdmlkZXIQARoOCgpzZWxlY3RlZEF0EAEaDAoIX19uYW1lX18QAQ
        at com.google.api.gax.rpc.ApiExceptionFactory.createException(ApiExceptionFactory.java:102)
        at com.google.api.gax.rpc.ApiExceptionFactory.createException(ApiExceptionFactory.java:41)
        at com.google.api.gax.grpc.GrpcApiExceptionFactory.create(GrpcApiExceptionFactory.java:86)
        at com.google.api.gax.grpc.GrpcApiExceptionFactory.create(GrpcApiExceptionFactory.java:66)
        at com.google.api.gax.grpc.ExceptionResponseObserver.onErrorImpl(ExceptionResponseObserver.java:82)
        at com.google.api.gax.rpc.StateCheckingResponseObserver.onError(StateCheckingResponseObserver.java:84)
        at com.google.api.gax.grpc.GrpcDirectStreamController$ResponseObserverAdapter.onClose(GrpcDirectStreamController.java:148)
        at io.grpc.PartialForwardingClientCallListener.onClose(PartialForwardingClientCallListener.java:39)
        at io.grpc.ForwardingClientCallListener.onClose(ForwardingClientCallListener.java:23)
        at io.grpc.ForwardingClientCallListener$SimpleForwardingClientCallListener.onClose(ForwardingClientCallListener.java:40)
        at com.google.api.gax.grpc.ChannelPool$ReleasingClientCall$1.onClose(ChannelPool.java:541)
        at io.grpc.internal.ClientCallImpl.closeObserver(ClientCallImpl.java:567)
        at io.grpc.internal.ClientCallImpl.access$300(ClientCallImpl.java:71)
        at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInternal(ClientCallImpl.java:735)
        at io.grpc.internal.ClientCallImpl$ClientStreamListenerImpl$1StreamClosed.runInContext(ClientCallImpl.java:716)
        at io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37)
        at io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133)
        ... 3 common frames omitted
Caused by: io.grpc.StatusRuntimeException: FAILED_PRECONDITION: The query requires an index. You can create it here: https://console.firebase.google.com/v1/r/project/gatherus-93c0a/firestore/indexes?create_composite=ClVwcm9qZWN0cy9nYXRoZXJ1cy05M2MwYS9kYXRhYmFzZXMvKGRlZmF1bHQpL2NvbGxlY3Rpb25Hcm91cHMvZ2F0aGVyaW5nU3BvdHMvaW5kZXhlcy9fEAEaCgoGY2l0eUlkEAEaDAoIcHJvdmlkZXIQARoOCgpzZWxlY3RlZEF0EAEaDAoIX19uYW1lX18QAQ
        at io.grpc.Status.asRuntimeException(Status.java:539)
        ... 14 common frames omitted
2026-05-10 15:46:42 - com.gather.job.GatheringSpotSyncJob - Found 0 spots selected in last 12 weeks
2026-05-10 15:46:42 - com.gather.job.GatheringSpotSyncJob - Filtered to 20 available spots (excluding recent selections)
2026-05-10 15:46:42 - com.gather.job.GatheringSpotSyncJob - Selected weekly gathering spot: Roam - 5105 Ballard Ave NW, Seattle, WA 98107, USA (Rating: 4.8)
2026-05-10 15:46:42 - c.g.r.GatheringSpotRepository - Saved gathering spot: Roam for city: VVkIK1UgU1fKYBpWgIsl
2026-05-10 15:46:42 - com.gather.job.GatheringSpotSyncJob - Saved gathering spot to Firestore
2026-05-10 15:46:42 - c.gather.repository.UserRepository - Found 0 users in city: VVkIK1UgU1fKYBpWgIsl
2026-05-10 15:46:42 - com.gather.job.GatheringSpotSyncJob - No users found for city VVkIK1UgU1fKYBpWgIsl, falling back to topic broadcast
2026-05-10 15:46:42 - c.g.service.PushNotificationService - Successfully sent topic push notification: projects/gatherus-93c0a/messages/528500154597489795
2026-05-10 16:04:12 - c.g.controller.AdminJobController - Manual trigger: weekly gathering spot job
2026-05-10 16:04:12 - com.gather.job.GatheringSpotSyncJob - Starting weekly gathering spot selection job using provider: google
2026-05-10 16:04:12 - com.gather.job.GatheringSpotSyncJob - Found 2 enabled cities in Firestore
2026-05-10 16:04:12 - com.gather.job.GatheringSpotSyncJob - Processing gathering spot for city: Seattle using google
2026-05-10 16:04:12 - c.g.service.GooglePlaceSearchService - Searching Google Places for 'bars' in 'Seattle, WA'
2026-05-10 16:04:12 - c.g.service.GooglePlacesApiService - Searching Google Places for query: bars in Seattle, WA, maxResults: 20
2026-05-10 16:04:12 - com.gather.job.GatheringSpotSyncJob - Processing gathering spot for city: Seattle using google
2026-05-10 16:04:12 - com.gather.job.GatheringSpotSyncJob - Exception during weekly gathering spot job
java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because the return value of "com.gather.model.domain.CityJobConfig.getSearchLimit()" is null
        at com.gather.job.GatheringSpotSyncJob.processCity(GatheringSpotSyncJob.java:104)
        at com.gather.job.GatheringSpotSyncJob.selectWeeklyGatheringSpot(GatheringSpotSyncJob.java:89)
        at com.gather.controller.AdminJobController.triggerWeeklyGather(AdminJobController.java:29)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:262)
        at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:190)
        at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:118)
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:917)
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:829)
        at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)
        at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089)
        at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:979)
        at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1014)
        at org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:914)
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:590)
        at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:885)
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:658)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:205)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149)
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:51)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149)
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149)
        at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149)
        at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149)
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:167)
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:90)
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:482)
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:115)
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93)
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74)
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:340)
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:391)
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63)
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:896)
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1744)
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52)
        at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191)
        at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659)
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
        at java.base/java.lang.Thread.run(Thread.java:1583)
2026-05-10 16:04:18 - c.g.service.GooglePlacesApiService - Successfully retrieved 20 places from Google Places API
2026-05-10 16:04:18 - c.g.service.GooglePlaceSearchService - Converted 20 Google Places to generic Place objects
2026-05-10 16:04:18 - com.gather.job.GatheringSpotSyncJob - Retrieved 20 potential gathering spots from google
2026-05-10 16:04:18 - com.gather.job.GatheringSpotSyncJob - Found 4 spots selected in last 12 weeks
2026-05-10 16:04:18 - com.gather.job.GatheringSpotSyncJob - Filtered to 18 available spots (excluding recent selections)
2026-05-10 16:04:18 - com.gather.job.GatheringSpotSyncJob - Selected weekly gathering spot: Old Stove Brewing Co — Pike Place - 1901 Western Ave, Seattle, WA 98101, USA (Rating: 4.5)
2026-05-10 16:04:18 - c.g.r.GatheringSpotRepository - Saved gathering spot: Old Stove Brewing Co — Pike Place for city: VVkIK1UgU1fKYBpWgIsl
2026-05-10 16:04:18 - com.gather.job.GatheringSpotSyncJob - Saved gathering spot to Firestore
2026-05-10 16:04:18 - c.gather.repository.UserRepository - Found 0 users in city: VVkIK1UgU1fKYBpWgIsl
2026-05-10 16:04:18 - com.gather.job.GatheringSpotSyncJob - No users found for city VVkIK1UgU1fKYBpWgIsl, falling back to topic broadcast
2026-05-10 16:04:18 - c.g.service.PushNotificationService - Successfully sent topic push notification: projects/gatherus-93c0a/messages/4209044234432276636
```

please debug and figure out what we need to fix and properly implement.

