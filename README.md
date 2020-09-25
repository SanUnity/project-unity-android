# PROJECT UNITY

El Project Unity es un projecto abierto con el que se pretende ayudar en la lucha contra el COVID-19. Dentro del proyecto, desde Android se pretende dar a los usuarios finales una herramienta para dispositivos Android. La parte nativa de la aplicación muestra en un WebView la webapp del front, gestionando los permisos e intercomunicación del front y la parte nativa. La App está gestionada por flavours, existiendo 3 en el proyecto:

* Centralized: Que contiene el sistema OpenTrace
* Descentralized: Que contiene la implementación del SDK DP3T
* Exposure: Que contiene la implementación del SDK de notificaciones de exposición de DP3T

### Configuración de los diferentes Flavours para su ejecución

El proyecto tiene añadidos dos SDKs de DP3T, uno llamado **sdk** y el otro **sdk_exposure**. 
Al cambiar a cualquiera de los diferentes flavours hay que tener en cuenta lo siguiente en el gradle para que la compilación sea correcta.

* Centralized: En el **build.gradle** de la app ha de descomentarse la línea 154, la cuál añade la biblioteca del Bluetooth de baja frecuencia.
* Decentralized: En el **build.gradle** se tiene que que descomentar la línea 127, permitiendo la adición del SDK a la compilación.  Además en el **settings.gradle** ha de quedar descomentada la inclusión del "sdk"
* Exposure: En el **build.gradle** se tiene que que descomentar la línea 129, permitiendo la adición del SDK a la compilación.  Además en el **settings.gradle** ha de quedar descomentada la inclusión del "sdk_exposure". Finalmente deberá comentarse la biblioteca comentada en el Flavour centralizado, ya que esta nos introduce permisos de Bluetooth, los cuales no son compatibles con las Notificaciones de Exposición del Google.

### Lenguaje de desarrollo

Kotlin 1.3.61

### Librerías utilizadas

Las siguientes bibliotecas son las más destacables dentro del proyecto.

Comunes:
* [RxPermisssions](com.github.tbruyelle:rxpermissions:0.10.2)
* [Koin](org.koin:koin-android)
* [retrofit2](com.squareup.retrofit2:retrofit)
* [firebase](com.google.firebase:firebase-analytics)

Dentro del flavour centralizado:
* [RxBLE](com.polidea.rxandroidble2:rxandroidble:1.10.1)
* [Room](androidx.room:room-runtime)

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

* SplashScreen, en la que se aprovecha para añadir diferentes comprobaciones de seguridad.
* MainActivity, donde se encuentra la WebView.

### Configuración:

La app tiene diferentes clases de configuración.

Los dos más importantes son:
 * Los strings de cada uno de los flavours, en los que se encuentra la información esencial que la app necesita para funcionar, como la URL de la webview, el nombre de la app, o los nombres alternativos para: realizar deep links.
 * El fichero de Constants que determina la url del Back End y la raíz de las URLs que se tomarán en consideración para el SSL Pinning.

Además podremos encontrar variables de configuración para el sistema de trackeo centralizado en el fichero **gradle.properties** 

### Conexión webapp/nativa:

El envío de información desde el JS a la parte nativa se realiza mediante una interfaz de JavaScript que se encuentra en el fichero **WebAppInterface.kt**, la cual es asignada a la WebView en la MainActivity cuando es creada.

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

### Descentralizado: BlueTrace Protocol (Modelo Singapur)
BlueTrace es un protocolo que preserva la privacidad para el rastreo de contactos impulsado por la comunidad mediante dispositivos Bluetooth, que permite la interoperabilidad global.

BlueTrace está diseñado para el registro de proximidad descentralizado y complementa el rastreo de contactos centralizado por parte de las autoridades de salud pública. El registro de proximidad mediante Bluetooth aborda una limitación clave del rastreo manual de contactos: que depende de la memoria de una persona y, por lo tanto, se limita a los contactos que una persona conoce y recuerda haber conocido. Por lo tanto, BlueTrace permite que el rastreo de contactos sea más escalable y requiera menos recursos.

### Centralizado: DP^3T (Modelo Suiza)
El proyecto Decentralized Privacy-Preserving Proximity Tracing (DP-3T) es un protocolo abierto para el rastreo de proximidad COVID-19 que utiliza la funcionalidad Bluetooth Low Energy en dispositivos móviles que garantiza que los datos personales permanezcan completamente en el teléfono de una persona. Fue elaborado por un equipo central de más de 25 científicos e investigadores académicos de toda Europa. También ha sido examinado y mejorado por la comunidad en general.

DP-3T es un esfuerzo independiente iniciado en EPFL y ETHZ que produjo este protocolo y que lo está implementando en una aplicación y un servidor de código abierto.)

### Exposure Notification API: Apple/Google Framework
La API de notificaciones de exposición es un esfuerzo conjunto entre Apple y Google para proporcionar la funcionalidad principal para crear aplicaciones iOS y Android para notificar a los usuarios de una posible exposición a casos confirmados de COVID-19.

### Exposure Notification API:

Para implementar la Exposure Notifications API la app necesitará los permisos de Google. 


## PROJECT UNITY

Project Unity is an open source project created with the intention to help in the fight against COVID-19. The native part of the application shows a WebView with a web application, managing its permissions and enabling a communication between this web-app and the native application. This app is divided in three flavors, this being:

* Centralized: Which contains OpenTrace
* Descentralized: Which contains the implementation of the SDK DP3T
* Exposure: Which contains the implementation of the SDK with exposure notification from DP3T

### Flavour configuration before execution

The project contains two DP3T SDKs, one called **sdk** and the other one **sdk_exposure**.
When swapping among the different flavors it is needed to keep in mind the next changes in order for the compilation to be correct:

* Centralized: In the **build.gradle** of the application the line 154 must be uncommented, this adds the BLE library.
* Decentralized: In the **build.gradle** the line 127 must be uncommented, allowing the SDK to be added during compilation.  It will also be required in **settings.gradle** to have uncommented the "sdk" inclusion.
* Exposure: In the **build.gradle** the line 129 must be uncommented, allowing the SDK for Exposure Notifications to be added during compilation.  It will also be required in **settings.gradle** to have uncommented the "sdk_exposure" inclusion. Finally the library specified in the centralized flavor needs to be commented, this is due to the library adding Bluetooth permissions to the application, which are not compatible with Google requirements for Exposure Notifications.

### Development Language

Kotlin 1.3.61

### Used Libraries

The next libraries are the more remarkable within the application:

Commons:
* [RxPermisssions](com.github.tbruyelle:rxpermissions:0.10.2)
* [Koin](org.koin:koin-android)
* [retrofit2](com.squareup.retrofit2:retrofit)
* [firebase](com.google.firebase:firebase-analytics)

Inside centralized Flavour:
* [RxBLE](com.polidea.rxandroidble2:rxandroidble:1.10.1)
* [Room](androidx.room:room-runtime)

### Required permissions

Depending on the selected flavor, the app may require the next permissions:

* ACCESS_NETWORK_STATE
* VIBRATE
* BLUETOOTH
* BLUETOOTH_ADMIN
* FOREGROUND_SERVICE
* RECEIVE_BOOT_COMPLETED
* ACCESS_COARSE_LOCATION
* READ_CONTACTS
* WRITE_EXTERNAL_STORAGE

### Required capabilities:

The application will always require to deactivate battery optimization when the DP3T SDK is enabled. This will be solicited before launching the tracing service.

### Native views:

The app implements three native views.

* SplashScreen, which is also used to add some security measures.
* MainActivity, where we can fain the WebView.

### Configuration:

The app contains different configuration classes.

The most important ones are:
 * The flavor related strings, where we can find the essential information that the app needs to work, like the Webview URL, the app name, the alternative names to use in deep links.
 * The Constants file that determines the Back End URL and the route of the URLs that will be used for SSL Pinning.

Also we may find different configuration variables for the centralized tracking system in the file **gradle.properties**

### Webapp/native connection:
The sending of information between JS and the native part of the application is done with a JS interface that is found in the file **WebAppInterface.kt**, this interface is assigned in the WebView in the MainActivity whenever it is created.

The sending of information between the native part of the application and the Webapp is done by JS calls from the WebView.

It is worth mentioning the interface functionalities:

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

### Centralized: BlueTrace Protocol (Singapur Model)
BlueTrace is a protocol that preserves user privacy in order to contact trace by Bluetooth devices, allowing a global interoperability.
BlueTrace is designed to be decentralized and it completes the centralized contact tracing by public health authorities. The proximity tracing with Bluetooth solves a key limitation in manual contact tracing, this being its reliance on a users memory this limits the contacts provided by an user. This issue being solve by BlueTrace allowing the system to be more scalable.

### Decentralized : DP3T (Switzeland Model)
The Decentralized Privacy-Preserving Proximity Tracing (DP-3T) is an open protocol for the proximity contact tracing COVID-19 which uses Bluetooth Low Energy functionality in mobile devices which ensures that personal data remains in its entirely on the users phone. I twas elaborated by a central team of more than 25 scientist and academy investigators from Europe. It has also been examined and improved by the whole community
DP-3T is independent effort started in EPFL and ETHZ, and it is being implemented in an application and an open source server.

### Exposure Notification API: Apple/Google Framework
The Exposure Notifications API is a collaborative effort developed between Apple and Google to provide the main functionality to create iOS and Android applications to notify users of a possible exposition to COVID-19 confirmed cases.

### Exposure Notification API:
To implement the Exposure Notifications API the app will require Google permission.

