import com.google.inject.AbstractModule

import java.time.Clock
import services.{ApplicationTimer, AtomicCounter, Counter, UserAuthorizationActor}
import daos._
import play.api.libs.concurrent.AkkaGuiceSupport

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule with AkkaGuiceSupport {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])
    //Set userDAO for public.user db connection
    bind(classOf[UserDao]).to(classOf[UserDaoSlick])
    //Set UserLogin for public.UserLogin db connection
    bind(classOf[UserLoginDao]).to(classOf[UserLoginSlick])
    //Set CategoryDao for public.Category db connection
    bind(classOf[CategoryDao]).to(classOf[CategoryDaoSlick])
    //Set UserConfigDao for user config tables db connection
    bind(classOf[UserConfigDao]).to(classOf[UserConfigDaoSlick])
    //Bind UserAuthorizationActor
    bindActor[UserAuthorizationActor]("user-authorization-actor")
  }

}
