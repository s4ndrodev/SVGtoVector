# SVG a Vector Drawable

Aplicación Android sencilla, escrita en **Java**, para **convertir archivos SVG a Vector Drawable (vector de Android)**.

Eliges el SVG, la app lo convierte y te devuelve el código XML, que puedes exportar o guardar en una carpeta.

---

## ¿Qué hace?

1. Permite **seleccionar un archivo SVG** del dispositivo.
2. Muestra una **vista previa** del SVG cargado para confirmar que es el correcto.
3. **Convierte** el SVG a un `<vector>` de Android.
4. Muestra el **código XML** resultante.
5. Permite **exportar** el XML a la ruta que elijas o **guardarlo** en una carpeta predefinida.

---

## Características

- Carga de SVG mediante el selector de archivos del sistema.
- Vista previa del SVG en un `ImageView`.
- Conversión a Vector Drawable con soporte para iconos que usan `fill="none"` y `stroke="currentColor"` (por ejemplo, los de [Lucide](https://lucide.dev)).
- Campo para el **nombre del archivo** de salida.
- Campo para el **color (tint)** del vector.
- Botón **Exportar**: guarda el XML en la ubicación que elijas en cada ocasión.
- Botón **Guardar**: guarda el XML directamente en una carpeta elegida previamente.
  - La carpeta se recuerda entre sesiones (preferencias).
  - Se puede **borrar la ruta guardada** para elegir otra.
- Interfaz con **Material 3** y **ViewBinding**.

---

## Formato de salida

El XML generado sigue esta estructura:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,2L2,7l10,5l10,-5z" />
</vector>
```

> El valor de `tint` puede cambiarse desde el campo de color de la app.

---

## Cómo usarla

1. Abre la app.
2. Pulsa **Cargar SVG** y selecciona el archivo.
3. Comprueba la vista previa.
4. Escribe el **nombre del archivo** y, si quieres, el **color (tint)**.
5. Revisa el XML generado.
6. Pulsa:
   - **Exportar** para elegir dónde guardar el archivo, o
   - **Guardar** para enviarlo a la carpeta configurada (la primera vez te pedirá elegirla).
7. Usa el archivo XML resultante donde lo necesites.

---

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Java |
| Plataforma | Android |
| UI | Material 3 |
| Binding | ViewBinding |
| Persistencia ligera | SharedPreferences (ruta de guardado) |
| Acceso a archivos | Storage Access Framework |

---

## Requisitos

- Android Studio (versión reciente)
- JDK 11 o superior
- Dispositivo o emulador con Android 7.0 (API 24) o superior *(ajusta según tu `minSdk`)*

---

## Compilar y ejecutar

```bash
git clone https://github.com/<tu-usuario>/svg-to-vector-drawable.git
cd svg-to-vector-drawable
```

1. Abre el proyecto en Android Studio.
2. Espera a que Gradle sincronice.
3. Ejecuta la app en un emulador o dispositivo físico (`Run ▶`).

---

## Limitaciones conocidas

- El Vector Drawable de Android soporta solo un subconjunto de SVG; elementos como filtros, degradados complejos o texto pueden no convertirse correctamente.
- Para mejores resultados, usa SVG simples (iconos) y, si es posible, con formas convertidas a `path`.

---

## Ideas a futuro

- Soporte para más elementos SVG (`circle`, `rect`, `line`, `polyline`, etc.).
- Botón para copiar el XML al portapapeles.
- Conversión por lotes de varios SVG.
- Ajuste del tamaño (`width` / `height`) y del `viewport`.

---

## Licencia

Define aquí la licencia de tu proyecto (por ejemplo, MIT).

---

## Autor

Desarrollado por **Alessandro**.
