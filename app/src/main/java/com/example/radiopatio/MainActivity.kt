package com.example.radiopatio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

// Importaciones necesarias para construir interfaces con Jetpack Compose
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

// LazyColumn permite crear listas cuyo contenido se carga bajo demanda
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// Formas que utilizaremos para los componentes visuales
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

// Iconos de Material
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share

// Componentes de Material 3
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

// Funcionalidades básicas de Compose
import androidx.compose.runtime.Composable

// Modificadores visuales
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

// Colores
import androidx.compose.ui.graphics.Color

// Tipografía
import androidx.compose.ui.text.font.FontWeight

// Unidades de medida
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tema que ha generado Android Studio para RadioPatio
import com.example.radiopatio.ui.theme.RadioPatioTheme


/**
 * Activity principal de la aplicación.
 *
 * Una Activity es uno de los puntos de entrada principales de una aplicación Android.
 *
 * En nuestro caso, MainActivity será la Activity que se encargará de iniciar
 * la interfaz de RadioPatio.
 */
class MainActivity : ComponentActivity() {

    /**
     * onCreate() se ejecuta cuando Android crea esta Activity.
     *
     * Aquí configuramos todo aquello que necesitamos para mostrar nuestra
     * aplicación.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        // Llamamos al comportamiento de la clase padre.
        super.onCreate(savedInstanceState)

        /*
         * enableEdgeToEdge() permite que nuestra interfaz pueda dibujarse
         * ocupando también las zonas cercanas a los bordes de la pantalla.
         *
         * Es una configuración habitual en aplicaciones Android modernas.
         */
        enableEdgeToEdge()

        /*
         * setContent{} es el punto donde comienza nuestra interfaz Jetpack Compose.
         *
         * Todo lo que coloquemos dentro de este bloque formará parte de la
         * interfaz gráfica que Android mostrará al usuario.
         */
        setContent {

            /*
             * RadioPatioTheme es el tema visual de nuestra aplicación.
             *
             * Android Studio nos ha generado este Theme.
             *
             * Más adelante modificaremos este tema para crear la identidad
             * visual propia de RadioPatio.
             */
            RadioPatioTheme {

                /*
                 * RadioPatioApp() será el punto de entrada de nuestra
                 * interfaz Compose.
                 */
                RadioPatioApp()
            }
        }
    }
}


/**
 * Data class que representa una publicación de RadioPatio.
 *
 * Una data class se utiliza principalmente para almacenar datos.
 *
 * En este caso cada objeto Post representa una publicación realizada
 * por un vecino.
 *
 * Por ejemplo:
 *
 * Post(
 *     author = "María",
 *     location = "Calle Mayor",
 *     time = "Hace 2 h",
 *     content = "Hoy hay reunión...",
 *     likes = 67,
 *     comments = 4
 * )
 *
 * Más adelante este modelo crecerá cuando conectemos RadioPatio
 * con nuestra base de datos.
 */
data class Post(

    // Nombre del usuario que ha realizado la publicación.
    val author: String,

    // Ubicación o comunidad desde la que se realiza la publicación.
    val location: String,

    // Tiempo transcurrido desde que se publicó.
    val time: String,

    // Texto que contiene la publicación.
    val content: String,

    // Número de "me gusta" que tiene la publicación.
    val likes: Int,

    // Número de comentarios que tiene la publicación.
    val comments: Int
)


/**
 * Función principal de la interfaz de RadioPatio.
 *
 * Las funciones marcadas con @Composable son funciones que pueden
 * construir elementos de interfaz utilizando Jetpack Compose.
 */
@Composable
fun RadioPatioApp() {

    /*
     * Actualmente nuestra aplicación solo tiene una pantalla:
     * el Feed.
     *
     * Más adelante esta función será mucho más importante porque
     * aquí podremos introducir la navegación entre:
     *
     * - Feed
     * - Comunidad
     * - Mercado
     * - Ranking
     * - Mensajes
     */
    FeedScreen()
}


/**
 * Pantalla principal de RadioPatio.
 *
 * Esta será inicialmente la pantalla del Feed.
 *
 * El Feed mostrará:
 *
 * 1. Barra superior
 * 2. Historias
 * 3. Zona para crear publicaciones
 * 4. Lista de publicaciones
 * 5. Navegación inferior
 */
@Composable
fun FeedScreen() {

    /*
     * Creamos una lista temporal de publicaciones.
     *
     * IMPORTANTE:
     *
     * Esto todavía NO es nuestra base de datos.
     *
     * Simplemente estamos creando datos de prueba para poder
     * desarrollar y visualizar la interfaz.
     *
     * Más adelante estos datos vendrán de nuestro repositorio
     * y posiblemente de un backend.
     */
    val posts = listOf(

        /*
         * Primera publicación de prueba.
         */
        Post(
            author = "María",
            location = "Calle Mayor",
            time = "Hace 2 h",
            content = "Hoy tenemos reunión de vecinos a las 19:00 en el portal.",
            likes = 67,
            comments = 4
        ),

        /*
         * Segunda publicación de prueba.
         */
        Post(
            author = "Juan",
            location = "Calle Mayor",
            time = "Hace 4 h",
            content = "¿Alguien tiene un taladro que pueda prestar este fin de semana?",
            likes = 8,
            comments = 3
        )
    )


    /*
     * Scaffold es uno de los componentes más importantes de Material 3.
     *
     * Nos proporciona una estructura básica para una pantalla de aplicación.
     *
     * Un Scaffold puede contener:
     *
     * - topBar       -> barra superior
     * - bottomBar    -> barra inferior
     * - floatingActionButton
     * - snackbarHost
     * - contenido principal
     *
     * Nosotros utilizaremos inicialmente topBar y bottomBar.
     */
    Scaffold(

        /*
         * Barra superior de RadioPatio.
         */
        topBar = {
            TopBar()
        },

        /*
         * Barra de navegación inferior.
         */
        bottomBar = {
            BottomNavigation()
        }

    ) { innerPadding ->

        /*
         * LazyColumn es una lista vertical que compone sus elementos
         * bajo demanda.
         *
         * Es especialmente útil para feeds porque un usuario puede
         * tener cientos o miles de publicaciones.
         *
         * En lugar de crear visualmente todas las publicaciones de golpe,
         * Compose va creando las que necesita mostrar.
         */
        LazyColumn(

            modifier = Modifier
                /*
                 * Ocupamos todo el espacio disponible.
                 */
                .fillMaxSize()

                /*
                 * Aplicamos el padding proporcionado por Scaffold.
                 *
                 * Esto evita que nuestro contenido quede debajo de
                 * la TopBar o BottomBar.
                 */
                .padding(innerPadding)
        ) {

            /*
             * Primer elemento de nuestra lista:
             * las historias.
             */
            item {

                Stories()
            }


            /*
             * Segundo elemento:
             * zona para crear una publicación.
             */
            item {

                CreatePost()
            }


            /*
             * Para cada objeto Post de nuestra lista:
             *
             * 1. obtenemos un Post
             * 2. lo llamamos post
             * 3. creamos una PostCard para mostrarlo
             */
            items(posts) { post ->

                PostCard(post)
            }
        }
    }
}


/**
 * Tarjeta visual que representa una publicación.
 *
 * Recibe un objeto Post como parámetro.
 *
 * Gracias a esto, la función no necesita saber de dónde vienen
 * los datos. Simplemente recibe una publicación y la representa.
 *
 * Esto es un principio muy importante de Compose:
 *
 * DATOS -> UI
 *
 * Más adelante podremos cambiar los datos sin tener que reescribir
 * completamente la interfaz.
 */
@Composable
fun PostCard(post: Post) {

    /*
     * Column coloca sus elementos verticalmente.
     *
     * En nuestra tarjeta tendremos:
     *
     * 1. Información del usuario
     * 2. Texto de la publicación
     * 3. Acciones
     */
    Column(

        modifier = Modifier
            .fillMaxWidth()

            /*
             * Separación entre publicaciones.
             */
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )

            /*
             * Redondeamos las esquinas de la tarjeta.
             */
            .clip(
                RoundedCornerShape(16.dp)
            )

            /*
             * Fondo de la tarjeta.
             */
            .background(
                MaterialTheme.colorScheme.surface
            )

            /*
             * Borde de la tarjeta.
             */
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )

            /*
             * Espacio interior de la tarjeta.
             */
            .padding(16.dp)
    ) {


        /*
         * Fila superior de la publicación.
         *
         * Contendrá:
         *
         * Avatar | Nombre + ubicación
         */
        Row(

            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * Avatar del usuario.
             *
             * De momento utilizamos simplemente un círculo.
             *
             * Más adelante sustituiremos esto por la fotografía
             * pública del perfil.
             */
            Box(

                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    )
            )


            /*
             * Espacio horizontal entre el avatar y los datos.
             */
            Spacer(
                modifier = Modifier.width(10.dp)
            )


            /*
             * Información del usuario.
             */
            Column(

                /*
                 * weight(1f) hace que esta columna ocupe el espacio
                 * restante disponible dentro del Row.
                 */
                modifier = Modifier.weight(1f)
            ) {

                /*
                 * Nombre del usuario.
                 */
                Text(
                    text = post.author,
                    fontWeight = FontWeight.Bold
                )

                /*
                 * Información secundaria:
                 *
                 * Hace 2 h · Calle Mayor
                 */
                Text(
                    text = "${post.time} · ${post.location}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        /*
         * Separación entre la cabecera y el contenido.
         */
        Spacer(
            modifier = Modifier.height(12.dp)
        )


        /*
         * Texto de la publicación.
         */
        Text(
            text = post.content,
            fontSize = 16.sp
        )


        /*
         * Separación entre el texto y las acciones.
         */
        Spacer(
            modifier = Modifier.height(12.dp)
        )


        /*
         * Fila inferior de acciones:
         *
         * Me gusta | Comentarios | Compartir
         */
        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween,

            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * Acción de "Me gusta".
             */
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Me gusta"
                )

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                Text(
                    text = "${post.likes}"
                )
            }


            /*
             * Acción de comentarios.
             */
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comentarios"
                )

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                Text(
                    text = "${post.comments}"
                )
            }


            /*
             * Acción de compartir.
             *
             * De momento es solamente visual.
             */
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Compartir"
            )
        }
    }
}


/**
 * Zona que permite al usuario comenzar a crear una publicación.
 *
 * Actualmente solamente es visual.
 *
 * Más adelante podremos convertirlo en un botón que abra
 * la pantalla de creación de publicaciones.
 */
@Composable
fun CreatePost() {

    Row(

        modifier = Modifier
            .fillMaxWidth()

            /*
             * Separación exterior.
             */
            .padding(16.dp)

            /*
             * Redondeamos la zona.
             */
            .clip(
                RoundedCornerShape(16.dp)
            )

            /*
             * Utilizamos el color secundario de la superficie
             * definido por nuestro MaterialTheme.
             */
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )

            /*
             * Espacio interior.
             */
            .padding(16.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        /*
         * Texto que invita al usuario a crear una publicación.
         *
         * weight(1f) hace que el texto ocupe todo el espacio
         * disponible antes del botón.
         */
        Text(
            text = "¿Qué quieres compartir hoy?",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )


        /*
         * De momento utilizamos un texto "+" como representación
         * temporal del botón de crear publicación.
         *
         * Más adelante utilizaremos un IconButton real.
         */
        Text(
            text = "+",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


/**
 * Zona superior del Feed destinada a las historias.
 *
 * Según los requisitos de RadioPatio, las historias son publicaciones
 * visuales que duran 24 horas.
 *
 * Actualmente solamente estamos creando los avatares de forma visual.
 */
@Composable
fun Stories() {

    /*
     * Lista temporal de usuarios que tienen historias.
     *
     * En el futuro esta información vendrá de nuestros datos reales.
     */
    val users = listOf(
        "Ana",
        "Carlos",
        "María",
        "Juan",
        "Tú"
    )


    /*
     * Row coloca todos los elementos horizontalmente.
     */
    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),

        /*
         * SpaceBetween distribuye los elementos intentando
         * aprovechar todo el ancho disponible.
         */
        horizontalArrangement = Arrangement.SpaceBetween
    ) {


        /*
         * forEach recorre todos los usuarios.
         *
         * Por cada usuario creamos un avatar y su nombre.
         */
        users.forEach { user ->

            /*
             * Cada historia tiene:
             *
             * Avatar
             * Nombre
             */
            Column(

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                /*
                 * Avatar circular.
                 *
                 * El borde utiliza el color principal del tema.
                 */
                Box(

                    modifier = Modifier
                        .size(58.dp)

                        /*
                         * Convertimos el Box en un círculo.
                         */
                        .clip(CircleShape)

                        /*
                         * Añadimos el borde circular.
                         */
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )

                        /*
                         * Espacio interior entre el borde y el contenido.
                         */
                        .padding(3.dp)
                ) {

                    /*
                     * Segundo Box que representa actualmente
                     * el fondo del avatar.
                     *
                     * Más adelante aquí colocaremos la imagen
                     * real del usuario.
                     */
                    Box(

                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            )
                    )
                }


                /*
                 * Espacio entre el avatar y el nombre.
                 */
                Spacer(
                    modifier = Modifier.height(4.dp)
                )


                /*
                 * Nombre del usuario.
                 */
                Text(
                    text = user,
                    fontSize = 12.sp
                )
            }
        }
    }
}


/**
 * Navegación inferior de RadioPatio.
 *
 * IMPORTANTE:
 *
 * Actualmente es solamente una representación visual.
 *
 * No hemos implementado todavía Navigation Compose.
 *
 * Cuando lleguemos a esa parte, cada elemento podrá navegar
 * realmente a una pantalla diferente.
 */
@Composable
fun BottomNavigation() {

    /*
     * Row permite colocar las diferentes secciones
     * horizontalmente.
     */
    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 12.dp
            ),

        /*
         * Distribuimos las opciones uniformemente.
         */
        horizontalArrangement = Arrangement.SpaceAround,

        verticalAlignment = Alignment.CenterVertically
    ) {

        /*
         * Sección Feed.
         */
        Text(
            text = "Feed",
            fontWeight = FontWeight.Bold
        )


        /*
         * Sección Comunidad.
         */
        Text(
            text = "Comunidad"
        )


        /*
         * Sección Mercado.
         */
        Text(
            text = "Mercado"
        )


        /*
         * Sección Ranking.
         */
        Text(
            text = "Ranking"
        )


        /*
         * Sección Mensajes.
         */
        Text(
            text = "Mensajes"
        )
    }
}


/**
 * Barra superior de RadioPatio.
 *
 * Contiene:
 *
 * - Nombre de la aplicación
 * - Botón de búsqueda
 * - Botón de notificaciones
 *
 * De momento los botones no tienen comportamiento.
 */
@Composable
fun TopBar() {

    /*
     * Row organiza horizontalmente los elementos de la barra.
     */
    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {

        /*
         * Nombre de RadioPatio.
         *
         * weight(1f) hace que el nombre ocupe todo el espacio
         * disponible antes de los botones.
         */
        Text(
            text = "RadioPatio",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.weight(1f)
        )


        /*
         * Botón de búsqueda.
         *
         * onClick todavía está vacío porque aún no hemos
         * implementado la búsqueda.
         */
        IconButton(
            onClick = {}
        ) {

            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Buscar"
            )
        }


        /*
         * Botón de notificaciones.
         *
         * También es solamente visual por ahora.
         */
        IconButton(
            onClick = {}
        ) {

            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notificaciones"
            )
        }
    }
}
