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

* SplashScreen, en la que se aprovecha para añadir.
* MainActivity, donde se encuentra la WebView.

### Configuración:

La app tiene diferentes clases de configuración para la app.

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

### Descentralizado: BlueTrace Protocol (Modelo Singaput)
BlueTrace es un protocolo que preserva la privacidad para el rastreo de contactos impulsado por la comunidad mediante dispositivos Bluetooth, que permite la interoperabilidad global.

BlueTrace está diseñado para el registro de proximidad descentralizado y complementa el rastreo de contactos centralizado por parte de las autoridades de salud pública. El registro de proximidad mediante Bluetooth aborda una limitación clave del rastreo manual de contactos: que depende de la memoria de una persona y, por lo tanto, se limita a los contactos que una persona conoce y recuerda haber conocido. Por lo tanto, BlueTrace permite que el rastreo de contactos sea más escalable y requiera menos recursos.

### Centralizado: DP^3T (Modelo Suiza
El proyecto Decentralized Privacy-Preserving Proximity Tracing (DP-3T) es un protocolo abierto para el rastreo de proximidad COVID-19 que utiliza la funcionalidad Bluetooth Low Energy en dispositivos móviles que garantiza que los datos personales permanezcan completamente en el teléfono de una persona. Fue elaborado por un equipo central de más de 25 científicos e investigadores académicos de toda Europa. También ha sido examinado y mejorado por la comunidad en general.

DP-3T es un esfuerzo independiente iniciado en EPFL y ETHZ que produjo este protocolo y que lo está implementando en una aplicación y un servidor de código abierto.)

### Exposure Notification API: Apple/Google Framework
La API de notificaciones de exposición es un esfuerzo conjunto entre Apple y Google para proporcionar la funcionalidad principal para crear aplicaciones iOS y Android para notificar a los usuarios de una posible exposición a casos confirmados de COVID-19.

### Exposure Notification API:

Para implementar la Exposure Notifications API la app necesitará los permisos de Google. 
