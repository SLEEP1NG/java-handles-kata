package none.cvg.variables;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static none.cvg.ErrorMessages.REFLECTION_FAILURE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/*
 * DONE:
 *  This test aims at differentiating VarHandles from the traditional Reflection/Unsafe usage.
 *  Each solved test shows how 'bad' things can be done with the traditional reflection calls.
 *  Each unsolved test proves that there is no support for the same in Method/VarHandles.
 *  The "ability" to modify finals and static finals using Unsafe is highlighted here. VarHandles
 *  do not support this behavior for very obvious reasons.
 */
public class SVarHandlesForbiddenUnsafeFeaturesTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void modifyPrivateFinalUsingReflection() {

        final ClassWithPrivateFinalField instance = new ClassWithPrivateFinalField(10);

        try {

            final Field privateFinalField = ClassWithPrivateFinalField.class.getDeclaredField(
                    "PRIVATE_FINAL_FIELD");

            // Needed for non-public fields
            privateFinalField.setAccessible(true);

            // VERY VERY BAD !!!
            // Will work (tested until Java 11), since the final modifier is ignored.
            privateFinalField.setInt(instance, 20);

            assertEquals("",
                    20,
                    instance.getPrivateFinalField());

        } catch (NoSuchFieldException | IllegalAccessException e) {

            fail(REFLECTION_FAILURE.getValue() + e.getMessage());

        }
    }

    @Test
    public void cannotModifyPrivateFinalUsingVarHandles() throws Throwable {

        /*
         * NOTE:
         * A call to alter the value of a final field should ideally fail, per the JEP / spec.
         * Final fields cannot be modified using VarHandles.
         */
        expectedException.expect(java.lang.Exception.class);

        final ClassWithPrivateFinalField instance = new ClassWithPrivateFinalField(10);

        /*
         * DONE:
         *  Replace the "null"s with valid values to get a VarHandle to PRIVATE_FINAL_FIELD.
         *  Note that the final field is in an inner class.
         *  Check API: java.lang.invoke.MethodHandles.privateLookupIn(?, ?)
         *             HINT: params are Target class, Lookup type
         *  Check API: java.lang.invoke.MethodHandles.Lookup.findVarHandle(?, ?, ?)
         *             HINT: params are Declaring class, Variable name, Variable type
         *  Remember PRIVATE_FINAL_FIELD in of type int.class
         */
        VarHandle privateFinalField = MethodHandles
                .privateLookupIn(SVarHandlesForbiddenUnsafeFeaturesTest.class,
                        MethodHandles.lookup())
                .findVarHandle(ClassWithPrivateFinalField.class,
                        "PRIVATE_FINAL_FIELD", int.class);

        /*
         * NOT ALLOWED:
         * Should throw the expected exception
         * Can throw either:
         *   java.lang.UnsupportedOperationException
         *     or
         *   java.lang.IllegalAccessException,
         * depending on call order.
         */
        privateFinalField.set(instance, 20);

    }

    @Test
    public void modifyConstantViaReflection() {

        final ClassWithPrivateFinalField instance = new ClassWithPrivateFinalField(10);

        try {

            assertEquals("",
                    10,
                    ClassWithPrivateFinalField.getConstant());

            Field publicStaticFinalConstant = ClassWithPrivateFinalField.class.getDeclaredField(
                    "CONSTANT");

            // Needed for non-public fields
            publicStaticFinalConstant.setAccessible(true);


            // Access the soecial "modifiers" field of Field.class
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            // VERY VERY BAD !!!
            // Alter the modifiers to remove the "final" modifier.
            modifiersField.setInt(publicStaticFinalConstant,
                    publicStaticFinalConstant.getModifiers() & ~Modifier.FINAL);


            // Will work, since the final modifier is removed.
            publicStaticFinalConstant.set(null, -20);

            assertEquals("",
                    -20,
                    ClassWithPrivateFinalField.getConstant());

        } catch (NoSuchFieldException | IllegalAccessException e) {

            fail(REFLECTION_FAILURE.getValue() + e.getMessage());

        }
    }

    @Test
    public void cannotModifyConstantUsingVarHandles() throws Throwable {

        /*
         * NOTE:
         * A call to alter the value of a constant should ideally fail, per the JEP / spec.
         * Constants (static finals) cannot be modified using VarHandles.
         */
        expectedException.expect(java.lang.UnsupportedOperationException.class);

        ClassWithPrivateFinalField instance = new ClassWithPrivateFinalField(10);

        /*
         * DONE:
         *  Replace the "null"s with valid values to get a VarHandle to PRIVATE_FINAL_FIELD.
         *  Note that the final field is in an inner class.
         *  Check API: java.lang.invoke.MethodHandles.privateLookupIn(?, ?)
         *             HINT: params are Target class, Lookup type
         *  Check API: java.lang.invoke.MethodHandles.Lookup.findVarHandle(?, ?, ?)
         *             HINT: params are Declaring class, Variable name, Variable type
         *  Remember CONSTANT in of type Integer.class
         */
        VarHandle publicStaticFinalConstant = MethodHandles
                .privateLookupIn(SVarHandlesForbiddenUnsafeFeaturesTest.class,
                        MethodHandles.lookup())
                .findStaticVarHandle(ClassWithPrivateFinalField.class,
                        "CONSTANT",
                        Integer.class);

        /*
         * NOT ALLOWED:
         * Should throw the expected exception.
         */
        publicStaticFinalConstant.set(instance, -20);

    }

    public static class ClassWithPrivateFinalField {

        public static final Integer CONSTANT = 10;

        private final int PRIVATE_FINAL_FIELD;

        ClassWithPrivateFinalField(final int field) {
            PRIVATE_FINAL_FIELD = field;
        }

        public static int getConstant() {
            return CONSTANT;
        }

        int getPrivateFinalField() {
            return PRIVATE_FINAL_FIELD;
        }
    }

}