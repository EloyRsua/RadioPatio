package com.example.radiopatio.comunidad

import com.example.radiopatio.informacionUsuario.ROL_ADMINISTRADOR_FINCAS
import com.example.radiopatio.informacionUsuario.ROL_PRESIDENTE
import com.example.radiopatio.informacionUsuario.ROL_VECINO
import com.example.radiopatio.informacionUsuario.Usuario
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar la consulta, creación, administración multi-comunidad
 * y traspasos en Cloud Firestore.
 */
class ComunidadRepository {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private fun esUsuarioDocAdministrador(doc: DocumentSnapshot): Boolean {
        val rol = doc.getString("role") ?: doc.getString("rol") ?: ""
        val tipo = doc.getString("tipoCuenta") ?: ""
        return rol.equals(ROL_ADMINISTRADOR_FINCAS, ignoreCase = true) ||
            tipo.equals(ROL_ADMINISTRADOR_FINCAS, ignoreCase = true)
    }

    private fun leerListaStrings(doc: DocumentSnapshot, campo: String): MutableList<String> {
        return (doc.get(campo) as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.toMutableList()
            ?: mutableListOf()
    }

    /**
     * Convierte un [DocumentSnapshot] de Firestore en un objeto [Comunidad].
     */
    fun documentToComunidad(doc: DocumentSnapshot): Comunidad {
        val comId = doc.getString("id")?.takeIf { it.isNotBlank() } ?: doc.id
        val adminUid = doc.getString("administradorUid")?.trim().orEmpty()
        val createdByUid = doc.getString("createdByUid") ?: ""
        val presidenteUid = doc.getString("presidenteUid")?.takeIf { it.isNotBlank() }
            ?: if (adminUid.isBlank()) createdByUid else ""
        val codigoDoc = doc.getString("codigo")?.trim()?.uppercase().orEmpty()

        return Comunidad(
            id = comId,
            nombre = doc.getString("nombre") ?: "",
            direccion = doc.getString("direccion") ?: "",
            ciudad = doc.getString("ciudad") ?: "",
            codigoPostal = doc.getString("codigoPostal") ?: "",
            descripcion = doc.getString("descripcion") ?: "",
            tipo = Comunidad.normalizarTipo(doc.getString("tipo")),
            numeroPisos = (doc.getLong("numeroPisos") ?: 0L).toInt().coerceAtLeast(0),
            totalVecinos = (doc.getLong("totalVecinos") ?: 0L).toInt().coerceAtLeast(0),
            photoUrl = doc.getString("photoUrl") ?: "",
            codigo = codigoDoc.ifBlank { Comunidad.codigoDesdeId(comId) },
            presidenteUid = presidenteUid,
            administradorUid = adminUid,
            createdByUid = createdByUid
        )
    }

    /**
     * Convierte una instancia de [Comunidad] en un mapa serializable para Firestore.
     */
    fun comunidadToMap(comunidad: Comunidad): Map<String, Any?> {
        val pisosFinales = if (comunidad.esEdificio) comunidad.numeroPisos.coerceAtLeast(0) else 0
        return mapOf(
            "id" to comunidad.id,
            "nombre" to comunidad.nombre,
            "direccion" to comunidad.direccion,
            "ciudad" to comunidad.ciudad,
            "codigoPostal" to comunidad.codigoPostal,
            "descripcion" to comunidad.descripcion,
            "tipo" to Comunidad.normalizarTipo(comunidad.tipo),
            "numeroPisos" to pisosFinales,
            "totalVecinos" to comunidad.totalVecinos,
            "photoUrl" to comunidad.photoUrl,
            "codigo" to comunidad.codigoCompartible,
            "presidenteUid" to comunidad.presidenteUid,
            "administradorUid" to comunidad.administradorUid,
            "createdByUid" to comunidad.createdByUid
        )
    }

    /**
     * Observa en tiempo real los datos de una comunidad concreta por su [comunidadId].
     * Si [comunidadId] está vacío, emite `null` inmediatamente sin consultar Firestore.
     */
    fun observarComunidadPorId(comunidadId: String): Flow<Comunidad?> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = db.collection(COLECCION_COMUNIDADES)
                .document(comunidadId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(documentToComunidad(snapshot))
                    } else {
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            trySend(null)
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Observa en tiempo real el catálogo de comunidades disponibles en Firestore.
     */
    fun observarComunidades(): Flow<List<Comunidad>> = callbackFlow {
        var listener: ListenerRegistration? = null
        try {
            listener = db.collection(COLECCION_COMUNIDADES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val lista = snapshot?.documents
                        ?.map { documentToComunidad(it) }
                        ?.sortedBy { it.nombre.lowercase() }
                        ?: emptyList()
                    trySend(lista)
                }
        } catch (e: Exception) {
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Consulta puntual de una comunidad por su identificador.
     */
    suspend fun obtenerComunidadPorId(comunidadId: String): Result<Comunidad?> {
        if (comunidadId.isBlank()) return Result.success(null)
        return try {
            val doc = db.collection(COLECCION_COMUNIDADES).document(comunidadId).get().await()
            if (doc.exists()) {
                Result.success(documentToComunidad(doc))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva comunidad en Firestore con ID autogenerado por la base de datos.
     * Solo los usuarios con rol de Administrador de fincas pueden crear comunidades.
     */
    suspend fun crearComunidad(
        nombre: String,
        direccion: String,
        ciudad: String = "",
        codigoPostal: String = "",
        descripcion: String = "",
        tipo: String = Comunidad.TIPO_URBANIZACION,
        numeroPisos: Int = 0,
        createdByUid: String = "",
        creadorEsAdministrador: Boolean = false
    ): Result<Comunidad> {
        val nombreLimpio = nombre.trim()
        val direccionLimpia = direccion.trim()
        if (nombreLimpio.isBlank() || direccionLimpia.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre y la dirección de la comunidad son obligatorios."))
        }
        val tipoNormalizado = Comunidad.normalizarTipo(tipo)
        val esEdificio = tipoNormalizado == Comunidad.TIPO_EDIFICIO
        val pisosFinales = if (esEdificio) numeroPisos else 0
        if (esEdificio && pisosFinales <= 0) {
            return Result.failure(IllegalArgumentException("Indica un número de pisos válido para el edificio."))
        }
        if (createdByUid.isBlank()) {
            return Result.failure(SecurityException("Solo los administradores de fincas pueden crear comunidades."))
        }

        return try {
            val userDoc = db.collection("users").document(createdByUid).get().await()
            val autorizado = if (userDoc.exists()) {
                esUsuarioDocAdministrador(userDoc)
            } else {
                creadorEsAdministrador
            }
            if (!autorizado) {
                return Result.failure(
                    SecurityException("Solo los administradores de fincas pueden crear comunidades.")
                )
            }

            val docRef = db.collection(COLECCION_COMUNIDADES).document()
            val codigoUnico = generarCodigoUnicoComunidad(docRef.id)
            val nuevaComunidad = Comunidad(
                id = docRef.id,
                nombre = nombreLimpio,
                direccion = direccionLimpia,
                ciudad = ciudad.trim(),
                codigoPostal = codigoPostal.trim(),
                descripcion = descripcion.trim().ifBlank { "Comunidad de vecinos de $nombreLimpio" },
                tipo = tipoNormalizado,
                numeroPisos = pisosFinales,
                totalVecinos = 0,
                photoUrl = "",
                codigo = codigoUnico,
                presidenteUid = "",
                administradorUid = createdByUid,
                createdByUid = createdByUid
            )
            docRef.set(comunidadToMap(nuevaComunidad)).await()
            Result.success(nuevaComunidad)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generarCodigoUnicoComunidad(fallbackId: String): String {
        repeat(5) {
            val candidato = Comunidad.generarCodigoAleatorio()
            val existente = db.collection(COLECCION_COMUNIDADES)
                .whereEqualTo("codigo", candidato)
                .limit(1)
                .get()
                .await()
            if (existente.isEmpty) return candidato
        }
        return Comunidad.codigoDesdeId(fallbackId)
    }

    /**
     * Permite al administrador de fincas de una comunidad de tipo Edificio dictaminar
     * o actualizar su número total de pisos.
     */
    suspend fun actualizarNumeroPisos(
        comunidadId: String,
        administradorUid: String,
        numeroPisos: Int
    ): Result<Unit> {
        if (comunidadId.isBlank() || administradorUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Datos de comunidad inválidos."))
        }
        if (numeroPisos <= 0) {
            return Result.failure(IllegalArgumentException("El edificio debe tener al menos 1 piso."))
        }

        return try {
            val comRef = db.collection(COLECCION_COMUNIDADES).document(comunidadId)
            db.runTransaction { transaction ->
                val comSnap = transaction.get(comRef)
                if (!comSnap.exists()) {
                    throw IllegalStateException("La comunidad no existe en Firestore.")
                }
                val adminEnDoc = comSnap.getString("administradorUid")?.trim().orEmpty()
                if (adminEnDoc != administradorUid) {
                    throw SecurityException("Solo el administrador de fincas de este edificio puede fijar los pisos.")
                }
                val tipoDoc = comSnap.getString("tipo").orEmpty()
                if (!tipoDoc.equals(Comunidad.TIPO_EDIFICIO, ignoreCase = true)) {
                    throw IllegalStateException("Solo las comunidades de tipo Edificio definen número de pisos.")
                }
                transaction.update(comRef, "numeroPisos", numeroPisos)
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Designa por primera vez o cambia de forma atómica al presidente de [comunidadId] hacia
     * [nuevoPresidenteUid]. ÚNICAMENTE el administrador de fincas actual de esa comunidad puede
     * ejecutar esta operación; nunca los usuarios ni el propio presidente.
     */
    suspend fun asignarOCambiarPresidencia(
        comunidadId: String,
        administradorUid: String,
        nuevoPresidenteUid: String,
        presidentePrevioFallbackUid: String = ""
    ): Result<Unit> {
        if (comunidadId.isBlank() || administradorUid.isBlank() || nuevoPresidenteUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Datos de presidencia incompletos."))
        }

        return try {
            val comRef = db.collection(COLECCION_COMUNIDADES).document(comunidadId)
            val adminUserRef = db.collection("users").document(administradorUid)
            val nuevoUserRef = db.collection("users").document(nuevoPresidenteUid)

            db.runTransaction { transaction ->
                val comSnap = transaction.get(comRef)
                val adminSnap = transaction.get(adminUserRef)
                val nuevoSnap = transaction.get(nuevoUserRef)

                if (!comSnap.exists()) {
                    throw IllegalStateException("La comunidad no existe en Firestore.")
                }
                if (!adminSnap.exists() || !nuevoSnap.exists()) {
                    throw IllegalStateException("Alguno de los usuarios no existe en Firestore.")
                }

                val adminEnComunidad = comSnap.getString("administradorUid")?.trim().orEmpty()
                if (adminEnComunidad != administradorUid || !esUsuarioDocAdministrador(adminSnap)) {
                    throw SecurityException("Solo el administrador de fincas de la comunidad puede designar o cambiar la presidencia.")
                }

                val nuevoEnCom = nuevoSnap.getString("comunidadId") == comunidadId ||
                    comunidadId in leerListaStrings(nuevoSnap, "comunidadesIds")
                if (!nuevoEnCom) {
                    throw SecurityException("El miembro elegido debe pertenecer a esta comunidad.")
                }
                if (esUsuarioDocAdministrador(nuevoSnap)) {
                    throw IllegalArgumentException("El administrador de fincas no puede ocupar la presidencia vecinal.")
                }

                val presAnteriorUid = comSnap.getString("presidenteUid")?.trim().orEmpty()
                    .ifBlank { presidentePrevioFallbackUid.trim() }
                if (presAnteriorUid == nuevoPresidenteUid) {
                    throw IllegalStateException("Este vecino ya ocupa la presidencia de la comunidad.")
                }

                val presAnteriorRef = if (presAnteriorUid.isNotBlank()) {
                    db.collection("users").document(presAnteriorUid)
                } else {
                    null
                }
                val presAnteriorSnap = presAnteriorRef?.let { transaction.get(it) }

                transaction.update(comRef, "presidenteUid", nuevoPresidenteUid)
                if (presAnteriorRef != null &&
                    presAnteriorSnap?.exists() == true &&
                    !esUsuarioDocAdministrador(presAnteriorSnap) &&
                    (presAnteriorSnap.getString("comunidadId") == comunidadId ||
                        comunidadId in leerListaStrings(presAnteriorSnap, "comunidadesIds"))
                ) {
                    transaction.update(presAnteriorRef, "role", ROL_VECINO)
                }
                transaction.update(nuevoUserRef, "role", ROL_PRESIDENTE)
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Escucha en tiempo real las solicitudes pendientes de traspaso de administración
     * dirigidas a la comunidad [comunidadId].
     */
    fun observarSolicitudesPendientesAdmin(comunidadId: String): Flow<List<SolicitudTraspasoAdmin>> = callbackFlow {
        if (comunidadId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var listener: ListenerRegistration? = null
        try {
            listener = db.collection(COLECCION_COMUNIDADES)
                .document(comunidadId)
                .collection(COLECCION_SOLICITUDES_ADMIN)
                .whereEqualTo("estado", SolicitudTraspasoAdmin.ESTADO_PENDIENTE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val lista = snapshot?.documents
                        ?.map { doc ->
                            SolicitudTraspasoAdmin(
                                id = doc.getString("id")?.takeIf { it.isNotBlank() } ?: doc.id,
                                comunidadId = doc.getString("comunidadId") ?: comunidadId,
                                comunidadNombre = doc.getString("comunidadNombre") ?: "",
                                solicitanteUid = doc.getString("solicitanteUid") ?: doc.id,
                                solicitanteNombre = doc.getString("solicitanteNombre") ?: "Administrador",
                                solicitanteEmail = doc.getString("solicitanteEmail") ?: "",
                                solicitantePhotoUrl = doc.getString("solicitantePhotoUrl") ?: "",
                                administradorActualUid = doc.getString("administradorActualUid") ?: "",
                                estado = doc.getString("estado") ?: SolicitudTraspasoAdmin.ESTADO_PENDIENTE,
                                timestamp = doc.getLong("timestamp") ?: 0L
                            )
                        }
                        ?.sortedByDescending { it.timestamp }
                        ?: emptyList()

                    trySend(lista)
                }
        } catch (_: Exception) {
            trySend(emptyList())
        }

        awaitClose {
            listener?.remove()
        }
    }

    /**
     * Registra una solicitud de traspaso para que [solicitante] pase a ser el único
     * administrador de fincas de [comunidadId] cuando el administrador actual la acepte.
     * Mientras tanto el solicitante permanece fuera de la comunidad.
     */
    suspend fun solicitarTraspasoAdmin(
        comunidadId: String,
        solicitante: Usuario
    ): Result<Unit> {
        if (comunidadId.isBlank() || solicitante.uid.isBlank()) {
            return Result.failure(IllegalArgumentException("Datos de solicitud incompletos."))
        }
        if (!solicitante.esAdministradorFincas) {
            return Result.failure(
                SecurityException("Solo los administradores de fincas pueden solicitar el traspaso de administración.")
            )
        }

        return try {
            val comRef = db.collection(COLECCION_COMUNIDADES).document(comunidadId)
            val solicitanteUserRef = db.collection("users").document(solicitante.uid)
            val solicitudRef = comRef.collection(COLECCION_SOLICITUDES_ADMIN).document(solicitante.uid)

            db.runTransaction { transaction ->
                val comSnap = transaction.get(comRef)
                val solicitanteSnap = transaction.get(solicitanteUserRef)
                val solicitudSnap = transaction.get(solicitudRef)

                if (!comSnap.exists()) {
                    throw IllegalStateException("La comunidad no existe.")
                }
                if (!solicitanteSnap.exists() || !esUsuarioDocAdministrador(solicitanteSnap)) {
                    throw SecurityException("Solo una cuenta de Administrador de fincas puede solicitar este traspaso.")
                }

                val adminActualUid = comSnap.getString("administradorUid")?.trim().orEmpty()
                if (adminActualUid.isBlank()) {
                    throw IllegalStateException("Esta comunidad todavía no tiene administrador asignado; puedes administrarla directamente.")
                }
                if (adminActualUid == solicitante.uid ||
                    comunidadId in leerListaStrings(solicitanteSnap, "comunidadesIds")
                ) {
                    throw IllegalStateException("Ya eres el administrador de fincas de esta comunidad.")
                }
                if (solicitudSnap.exists() &&
                    solicitudSnap.getString("estado") == SolicitudTraspasoAdmin.ESTADO_PENDIENTE
                ) {
                    throw IllegalStateException("Ya tienes una solicitud pendiente para esta comunidad.")
                }

                val pendientes = leerListaStrings(solicitanteSnap, "solicitudesPendientesComunidades")
                if (comunidadId !in pendientes) {
                    pendientes.add(comunidadId)
                }

                val payload = mapOf(
                    "id" to solicitante.uid,
                    "comunidadId" to comunidadId,
                    "comunidadNombre" to (comSnap.getString("nombre") ?: ""),
                    "solicitanteUid" to solicitante.uid,
                    "solicitanteNombre" to solicitante.nombre,
                    "solicitanteEmail" to solicitante.email,
                    "solicitantePhotoUrl" to solicitante.photoUrl,
                    "administradorActualUid" to adminActualUid,
                    "estado" to SolicitudTraspasoAdmin.ESTADO_PENDIENTE,
                    "timestamp" to System.currentTimeMillis()
                )

                transaction.set(solicitudRef, payload)
                transaction.update(solicitanteUserRef, "solicitudesPendientesComunidades", pendientes)
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Acepta de forma atómica la solicitud de [solicitanteUid]:
     * el administrador saliente ([administradorActualUid]) deja de administrar y pertenecer
     * a [comunidadId], y el solicitante pasa a ser su único Administrador de fincas.
     */
    suspend fun aceptarSolicitudTraspasoAdmin(
        comunidadId: String,
        administradorActualUid: String,
        solicitanteUid: String
    ): Result<Unit> {
        if (comunidadId.isBlank() || administradorActualUid.isBlank() || solicitanteUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Datos de aceptación incompletos."))
        }

        return try {
            val comRef = db.collection(COLECCION_COMUNIDADES).document(comunidadId)
            val adminActualRef = db.collection("users").document(administradorActualUid)
            val solicitanteRef = db.collection("users").document(solicitanteUid)
            val solicitudRef = comRef.collection(COLECCION_SOLICITUDES_ADMIN).document(solicitanteUid)

            db.runTransaction { transaction ->
                val comSnap = transaction.get(comRef)
                val adminActualSnap = transaction.get(adminActualRef)
                val solicitanteSnap = transaction.get(solicitanteRef)
                val solicitudSnap = transaction.get(solicitudRef)

                if (!comSnap.exists() || !adminActualSnap.exists() || !solicitanteSnap.exists() || !solicitudSnap.exists()) {
                    throw IllegalStateException("La solicitud o alguno de los usuarios ya no existe.")
                }

                val adminEnComunidad = comSnap.getString("administradorUid")?.trim().orEmpty()
                if (adminEnComunidad != administradorActualUid) {
                    throw SecurityException("Solo el administrador actual de la comunidad puede aceptar el traspaso.")
                }
                if (solicitudSnap.getString("estado") != SolicitudTraspasoAdmin.ESTADO_PENDIENTE) {
                    throw IllegalStateException("La solicitud ya fue resuelta anteriormente.")
                }
                if (!esUsuarioDocAdministrador(solicitanteSnap)) {
                    throw SecurityException("El solicitante debe ser Administrador de fincas.")
                }

                val comNombre = comSnap.getString("nombre") ?: ""
                val comunidadesSaliente = leerListaStrings(adminActualSnap, "comunidadesIds")
                    .filter { it != comunidadId }
                val activaSaliente = adminActualSnap.getString("comunidadId").orEmpty()
                val siguienteActivaSaliente = if (activaSaliente == comunidadId) {
                    comunidadesSaliente.firstOrNull().orEmpty()
                } else {
                    activaSaliente
                }

                val siguienteComSnap = if (activaSaliente == comunidadId && siguienteActivaSaliente.isNotBlank()) {
                    transaction.get(db.collection(COLECCION_COMUNIDADES).document(siguienteActivaSaliente))
                } else {
                    null
                }

                val comunidadesEntrante = leerListaStrings(solicitanteSnap, "comunidadesIds")
                if (comunidadId !in comunidadesEntrante) {
                    comunidadesEntrante.add(comunidadId)
                }
                val pendientesEntrante = leerListaStrings(solicitanteSnap, "solicitudesPendientesComunidades")
                    .filter { it != comunidadId }

                // Actualizar comunidad: nuevo administrador único
                val cambiosCom = mutableMapOf<String, Any>(
                    "administradorUid" to solicitanteUid
                )
                if (comSnap.getString("presidenteUid") == administradorActualUid) {
                    cambiosCom["presidenteUid"] = ""
                }
                transaction.update(comRef, cambiosCom)

                // El administrador actual sale de esta comunidad
                val cambiosSaliente = mutableMapOf<String, Any>(
                    "comunidadesIds" to comunidadesSaliente,
                    "role" to ROL_ADMINISTRADOR_FINCAS,
                    "tipoCuenta" to ROL_ADMINISTRADOR_FINCAS
                )
                if (activaSaliente == comunidadId) {
                    cambiosSaliente["comunidadId"] = siguienteActivaSaliente
                    cambiosSaliente["comunidadNombre"] = siguienteComSnap?.getString("nombre").orEmpty()
                }
                transaction.update(adminActualRef, cambiosSaliente)

                // El solicitante entra como nuevo administrador único
                val cambiosEntrante = mutableMapOf<String, Any>(
                    "comunidadesIds" to comunidadesEntrante,
                    "solicitudesPendientesComunidades" to pendientesEntrante,
                    "role" to ROL_ADMINISTRADOR_FINCAS,
                    "tipoCuenta" to ROL_ADMINISTRADOR_FINCAS
                )
                if (solicitanteSnap.getString("comunidadId").isNullOrBlank()) {
                    cambiosEntrante["comunidadId"] = comunidadId
                    cambiosEntrante["comunidadNombre"] = comNombre
                }
                transaction.update(solicitanteRef, cambiosEntrante)

                transaction.update(
                    solicitudRef,
                    mapOf(
                        "estado" to SolicitudTraspasoAdmin.ESTADO_ACEPTADA,
                        "fechaResolucion" to System.currentTimeMillis()
                    )
                )
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rechaza una solicitud pendiente de traspaso de administración.
     */
    suspend fun rechazarSolicitudTraspasoAdmin(
        comunidadId: String,
        administradorActualUid: String,
        solicitanteUid: String
    ): Result<Unit> {
        return resolverSolicitudSinTraspaso(
            comunidadId = comunidadId,
            solicitanteUid = solicitanteUid,
            nuevoEstado = SolicitudTraspasoAdmin.ESTADO_RECHAZADA,
            administradorEsperadoUid = administradorActualUid
        )
    }

    /**
     * Cancela una solicitud pendiente enviada por el propio solicitante.
     */
    suspend fun cancelarSolicitudTraspasoAdmin(
        comunidadId: String,
        solicitanteUid: String
    ): Result<Unit> {
        return resolverSolicitudSinTraspaso(
            comunidadId = comunidadId,
            solicitanteUid = solicitanteUid,
            nuevoEstado = SolicitudTraspasoAdmin.ESTADO_CANCELADA,
            administradorEsperadoUid = null
        )
    }

    private suspend fun resolverSolicitudSinTraspaso(
        comunidadId: String,
        solicitanteUid: String,
        nuevoEstado: String,
        administradorEsperadoUid: String?
    ): Result<Unit> {
        if (comunidadId.isBlank() || solicitanteUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Datos de solicitud incompletos."))
        }

        return try {
            val comRef = db.collection(COLECCION_COMUNIDADES).document(comunidadId)
            val solicitanteRef = db.collection("users").document(solicitanteUid)
            val solicitudRef = comRef.collection(COLECCION_SOLICITUDES_ADMIN).document(solicitanteUid)

            db.runTransaction { transaction ->
                val comSnap = transaction.get(comRef)
                val solicitanteSnap = transaction.get(solicitanteRef)
                val solicitudSnap = transaction.get(solicitudRef)

                if (administradorEsperadoUid != null) {
                    val adminEnCom = comSnap.getString("administradorUid")?.trim().orEmpty()
                    if (adminEnCom != administradorEsperadoUid) {
                        throw SecurityException("Solo el administrador actual puede responder a esta solicitud.")
                    }
                }

                if (solicitudSnap.exists()) {
                    transaction.update(
                        solicitudRef,
                        mapOf(
                            "estado" to nuevoEstado,
                            "fechaResolucion" to System.currentTimeMillis()
                        )
                    )
                }
                if (solicitanteSnap.exists()) {
                    val pendientes = leerListaStrings(solicitanteSnap, "solicitudesPendientesComunidades")
                        .filter { it != comunidadId }
                    transaction.update(solicitanteRef, "solicitudesPendientesComunidades", pendientes)
                }
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ajusta de forma atómica el contador de vecinos de la comunidad indicada.
     */
    suspend fun ajustarContadorVecinos(comunidadId: String, delta: Long) {
        if (comunidadId.isBlank()) return
        try {
            db.collection(COLECCION_COMUNIDADES)
                .document(comunidadId)
                .update("totalVecinos", FieldValue.increment(delta))
                .await()
        } catch (_: Exception) {
            // Si el documento aún no existe o no permite incremento, se ignora sin bloquear el flujo principal
        }
    }

    companion object {
        const val COLECCION_COMUNIDADES = "comunidades"
        const val COLECCION_SOLICITUDES_ADMIN = "solicitudes_admin"
    }
}
