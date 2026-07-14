package io.argus.index;

import io.argus.document.Document;
import io.argus.document.Field;
import io.argus.document.FieldType;
import io.argus.store.IndexInput;
import io.argus.store.IndexOutput;
import java.util.List;

/** Serializes and deserializes a {@link Document}'s stored fields (name, type, value, boost). */
public final class DocumentCodec {

    private static final FieldType[] TYPES = FieldType.values();

    private DocumentCodec() {
    }

    public static void write(IndexOutput out, Document doc) {
        List<Field> fields = doc.fields();
        out.writeVInt(fields.size());
        for (Field f : fields) {
            out.writeString(f.name());
            out.writeByte(f.type().ordinal());
            out.writeString(f.value());
            out.writeInt(Float.floatToIntBits(f.boost()));
        }
    }

    public static Document read(IndexInput in) {
        int n = in.readVInt();
        Document doc = new Document();
        for (int i = 0; i < n; i++) {
            String name = in.readString();
            int typeOrdinal = in.readByte() & 0xFF;
            String value = in.readString();
            float boost = Float.intBitsToFloat(in.readInt());
            doc.add(new Field(name, value, TYPES[typeOrdinal], boost));
        }
        return doc;
    }
}
