package fr.dynamx.common.contentpack.loader;

import fr.aym.acslib.api.services.ErrorTrackingService;
import fr.dynamx.api.contentpack.object.INamedObject;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.common.DynamXContext;
import fr.dynamx.utils.DynamXLoadingTasks;
import fr.dynamx.utils.doc.ContentPackDocGenerator;
import fr.dynamx.utils.doc.DocLocale;

import java.lang.reflect.Field;

/**
 * Represents a configuration field, contains the corresponding field, and the "parser" ({@link DefinitionType}) able to translate the string value of this property into the correct type
 *
 * @param <T> The corresponding class field type
 * @see fr.dynamx.api.contentpack.registry.PackFileProperty
 */
public class PackFilePropertyData<T> {
    private final Field field;
    private final String configFieldName;
    private final DefinitionType<T> type;
    private final boolean required;
    private final String usage;
    private final String description;
    private final String defaultValue;

    public PackFilePropertyData(Field field, String configFieldName, DefinitionType<T> type, boolean required, String usage, String description, String defaultValue) {
        this.field = field;
        this.configFieldName = configFieldName;
        this.type = type;
        this.required = required;
        this.usage = usage;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public boolean isDeprecated() {
        return usage != null;
    }

    public boolean isRequired() {
        return required && !isDeprecated();
    }

    /**
     * Parses the provided config value (string format) into the appropriate object to affect it to the class field
     */
    public T parse(String value) {
        return type.getValue(value);
    }

    public Field getField() {
        return field;
    }

    /**
     * @return The name of this property in the config file
     */
    public String getConfigFieldName() {
        return configFieldName;
    }

    /**
     * @return The parser of this property
     */
    public DefinitionType<T> getType() {
        return type;
    }

    /**
     * Parses the string value of this field an injects it into the given object (assuming it has the java field)
     *
     * @param on    The loading object
     * @param value The string value of this property
     * @return The corresponding, not deprecated, property data, or null if parse failed
     * @throws IllegalAccessException If reflection fails
     */
    @SuppressWarnings("unchecked")
    public PackFilePropertyData<T> apply(INamedObject on, String value) throws IllegalAccessException {
        T val;
        try {
            val = parse(value);
        } catch (Exception e) {
            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, on.getPackName(), "Cannot parse property " + getConfigFieldName() + " of " + on.getName() + ". Make sure to respect the syntax given in the documentation.", e, ErrorTrackingService.TrackedErrorLevel.HIGH);
            return null; //Error while parsing
        }
        field.setAccessible(true);
        field.set(on, val);
        field.setAccessible(false);
        if (isDeprecated()) {
            DynamXContext.getErrorTracker().addError(DynamXLoadingTasks.PACK, on.getPackName(), on.getName(), "Deprecated config key found " + configFieldName + ". You should now use " + usage, ErrorTrackingService.TrackedErrorLevel.LOW);
            PackFilePropertyData<T> data = (PackFilePropertyData<T>) SubInfoTypeAnnotationCache.getFieldFor(on, usage);
            if (data != null)
                return data;
        }
        return this;
    }

    public void writeDocLine(StringBuilder builder, DocLocale locale, ContentPackDocGenerator.DocType type) {
        if (isDeprecated()) {
            if (type != ContentPackDocGenerator.DocType.DEPRECATED)
                return;
        } else if (isRequired()) {
            if (type != ContentPackDocGenerator.DocType.REQUIRED)
                return;
        } else {
            if (type != ContentPackDocGenerator.DocType.OPTIONAL)
                return;
        }
        String docKey = description.isEmpty() ? field.getDeclaringClass().getSimpleName() + "." + configFieldName : description;
        if(isDeprecated()) {
            if(!locale.hasKey(docKey)) {
                docKey = "common.error.deprecated";
            }
        }
        String sep = "|";
        String typeName = locale.format(this.type.getTypeName());
        builder.append(sep).append(type).append(sep).append(configFieldName).append(sep).append(typeName).append(sep)
                .append(locale.format(docKey)).append(sep).append(defaultValue.isEmpty() ? "   " : defaultValue).append(sep).append("\n");
    }
}
