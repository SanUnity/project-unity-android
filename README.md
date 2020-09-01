# PROJECT UNITY

El Project Unity es un projecto abierto con el que se pretende ayudar en la lucha contra el COVID-19. Dentro del proyecto, desde Android se pretende dar a los usuarios finales una herramienta para dispositivos Android. La parte nativa de la aplicación muestra en un WebView la webapp del front, gestionando los permisos e intercomunicación del front y la parte nativa. La App está gestionada por flavours, existiendo 3 en el proyecto:

* Centralized: Que contiene el sistema OpenTrace
* Descentralized: Que contiene la implementación del SDK DP3T
* Exposure: Que contiene la implementación del SDK de notificaciones de exposición de DP3T

### Configuración de los diferentes Flavours para su ejecución

El proyecto tiene añadido dos SDKs de DP3T, uno llamado **sdk** y el otro **sdk_exposure**. 
Al cambiar a cualquiera de los diferentes flavours hay que tener en cuenta lo siguiente en el gradle para que la compilación sea correcta.

* Centralized: En el **build.gradle** de la app ha de descomentarse la línea 154, la cuál añade la biblioteca del Bluetooth de baja frecuencia.
* Decentralized: En el **build.gradle** se tiene que que descomentar la línea 127, permitiendo la adición del SDK a la compilación.  Además en el **settings.gradle** ha de quedar descomentada la inclusión del "sdk"
* Exposure: En el **build.gradle** se tiene que que descomentar la línea 129, permitiendo la adición del SDK a la compilación.  Además en el **settings.gradle** ha de quedar descomentada la inclusión del "sdk_exposure". Finalmente deberá comentarse la biblioteca comentada en el Flavour centralizado, ya que esta nos introduce permisos de Bluetooth, los cuales no son compatibles con las Notificaciones de Exposición del Google.

### Lenguaje de desarrollo

Kotlin 1.3.61

### Librerías utilizadas

Las siguientes bibliotecas son las más destacables dentro del proyecto.

**************************************************** WIP ****************************************************************************
Comunes:
* [RxPermisssions](com.github.tbruyelle:rxpermissions:0.10.2)
* [Koin](org.koin:koin-android)


Dentro del flavour centralizado:
* [RxBLE](com.polidea.rxandroidble2:rxandroidble:1.10.1)
* [Room](androidx.room:room-runtime)

Dentro del flavour descentralizado:
* [DP-3T](https://github.com/DP-3T/dp3t-sdk-ios)
* [SwiftKeychainWrapper](https://github.com/jrendel/SwiftKeychainWrapper)
* [TrustKit](https://github.com/datatheorem/TrustKit)
* [ReachabilitySwift](https://github.com/ashleymills/Reachability.swift)
* [SQLite](https://github.com/stephencelis/SQLite.swift)
* [SwiftProtobuf](https://github.com/apple/swift-protobuf)
* [SwiftJWT](https://github.com/IBM-Swift/Swift-JWT)

Dentro del flavour de las exposure notification:

**************************************************** WIP ****************************************************************************

### Permisos requeridos

La app en función del flavour pide diferente permisos, pudiendo llegar estos a ser:

* ACCESS_NETWORK_STATE
* VIBRATE
* BLUETOOTH
* BLUETOOTH_ADMIN
* FOREGROUND_SERVICE
* RECEIVE_BOOT_COMPLETED
* ACCESS_COARSE_LOCATION
* READ_CONTACTS
* WRITE_EXTERNAL_STORAGE

### Capacidades requeridas:

La aplicación requerirá siempre que se utilice el SDK de DP3T que se desactive la optimización de batería. Esto será solicitado siempre antes de lanzar el servicio de tracing.

### Vistas nativas:

La app implenta estas tres vistas nativas.

* SplashScreen, en la que se aprovecha para añadir.
* MainActivity, donde se encuentra la WebView.

### Configuración:

La app tiene diferentes clases de configuración para la app.

Los dos más importantes son:
 * Los strings de cada uno de los flavours, en los que se encuentra la información esencial que la app necesita para funcionar, como la URL de la webview, el nombre de la app, o los nombres alternativos para: realizar deep links.
 * El fichero de Constants que determina la url del Back End y la raíz de las URLs que se tomarán en consideración para el SSL Pinning.

Además podremos encontrar variables de configuración para el sistema de trackeo centralizado en el fichero **gradle.properties** 

### Conexión webapp/nativa:

El envío de información desde el JS a la parte nativa se realiza mediante una interfaz de JavaScript que se encuentra en el fichero **WebAppInterface.kt**, la cual es asignada a la WebView en la MainActivity cuando se es creada.

El envío de información desde la parte nativa a la webapp se realiza mediante llamadas al JS del WebView

Caben a destacar las siguientes funcionalidades de la interfaz:

* postData
* requestBT
* startBluetooth 
* getBTDataFromApp 
* syncExposedUser 
* shareApp 
* stopExposedNotifications 
* logout 
* getToken 
* getStatus 
* openAppSettings 
* getDeviceToken
* share

### Exposure Notification API:

Para implementar la Exposure Notifications API la app necesitará los permisos de Google. 
