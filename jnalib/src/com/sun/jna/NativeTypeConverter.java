package com.sun.jna;

/**
 * Type converter to map native types.
 *
 * @author stefan@endrullis.de
 */
public abstract class NativeTypeConverter implements TypeConverter {
    protected Class nativeType;
    protected boolean array = false;

    public static NativeTypeConverter getInstance(Class mapper, Class mappedClass) {
        NativeTypeConverter nmc = null;
        try {
            nmc = (NativeTypeConverter) mapper.newInstance();
            nmc.setMappedClass(mappedClass);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return nmc;
    }

    protected void setNativeType(Class nativeType) {
        this.nativeType = nativeType;
        this.array = nativeType.isArray();
    }

    public void setMappedClass(Class mappedClass){
    }

    public static class NativeLong extends NativeTypeConverter {
        private final boolean d64 = com.sun.jna.NativeLong.SIZE == 8;

        public void setMappedClass(Class mappedClass) {
            if (mappedClass.isArray()) {
                 setNativeType(d64 ? long[].class : int[].class);
            } else {
                setNativeType(d64 ? Long.class : Integer.class);
            }
        }

        public Class nativeType() {
            return nativeType;
        }

        public Object fromNative(Object nativeValue, Object field, FromNativeContext context) {
            if (d64) return nativeValue;

            if (!array) {
                return new Long((Integer) nativeValue);
            } else {
                int[] ints = (int[]) nativeValue;
                long[] longs = (long[]) field;
                for (int i = 0; i < longs.length; i++) {
                    longs[i] = ints[i];
                }
                return longs;
            }
        }

        public Object toNative(Object value, ToNativeContext context) {
            if (d64) return value;

            if (!array) {
                return new Integer(((Long) value).intValue());
            } else {
                long[] longs = (long[]) value;
                int[] ints = new int[longs.length];
                for (int i = 0; i < longs.length; i++) {
                    ints[i] = (int) longs[i];
                }
                return ints;
            }
        }
    }
}
