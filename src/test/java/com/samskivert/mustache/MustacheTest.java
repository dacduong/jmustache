//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Mustache tests that can only be run on the JVM. Most tests should go in BaseMustacheTest so
 * that they can be run on GWT and the JVM to ensure they work in both places.
 */
public class MustacheTest extends SharedTests
{
    @Test public void testFieldVariable () {
        test("bar", "{{foo}}", new Object() {
            String foo = "bar";
        });
    }

    @Test public void testMethodVariable () {
        test("bar", "{{foo}}", new Object() {
            String foo () { return "bar"; }
        });
    }

    @Test public void testPropertyVariable () {
        test("bar", "{{foo}}", new Object() {
            String getFoo () { return "bar"; }
        });
    }

    public interface HasDefault {
        default String getFoo () { return "bar"; }
    }
    public interface Interloper extends HasDefault {
        default String getFoo () { return "bang"; }
    }
    @Test public void testDefaultMethodVariable () {
        test("bar", "{{foo}}", new HasDefault() {
        });
        test("bang", "{{foo}}", new Interloper() {
        });
        test("bong", "{{foo}}", new Interloper() {
            public String getFoo () { return "bong"; }
        });
    }

    @Test public void testBooleanPropertyVariable () {
        test("true", "{{foo}}", new Object() {
            Boolean isFoo () { return true; }
        });
    }

    @Test public void testPrimitiveBooleanPropertyVariable () {
        test("false", "{{foo}}", new Object() {
            boolean isFoo () { return false; }
        });
    }

    @Test public void testSkipVoidReturn () {
        test("bar", "{{foo}}", new Object() {
            void foo () {}
            String getFoo () { return "bar"; }
        });
    }

    @Test public void testCallSiteReuse () {
        Template tmpl = Mustache.compiler().compile("{{foo}}");
        Object ctx = new Object() {
            String getFoo () { return "bar"; }
        };
        for (int ii = 0; ii < 50; ii++) {
            check("bar", tmpl.execute(ctx));
        }
    }

    @Test public void testCallSiteChange () {
        Template tmpl = Mustache.compiler().compile("{{foo}}");
        check("bar", tmpl.execute(new Object() {
            String getFoo () { return "bar"; }
        }));
        check("bar", tmpl.execute(new Object() {
            String foo = "bar";
        }));
    }

    @Test public void testSectionWithNonFalseyZero () {
        test(Mustache.compiler(), "test", "{{#foo}}test{{/foo}}", new Object() {
            Long foo = 0L;
        });
    }

    @Test public void testSectionWithFalseyZero () {
        test(Mustache.compiler().zeroIsFalse(true), "",
             "{{#intv}}intv{{/intv}}" +
             "{{#longv}}longv{{/longv}}" +
             "{{#floatv}}floatv{{/floatv}}" +
             "{{#doublev}}doublev{{/doublev}}" +
             "{{#longm}}longm{{/longm}}" +
             "{{#intm}}intm{{/intm}}" +
             "{{#floatm}}floatm{{/floatm}}" +
             "{{#doublem}}doublem{{/doublem}}",
             new Object() {
                 Integer intv = 0;
                 Long longv = 0L;
                 Float floatv = 0f;
                 Double doublev = 0d;
                 int intm () { return 0; }
                 long longm () { return 0l; }
                 float floatm () { return 0f; }
                 double doublem () { return 0d; }
             });
    }

    @Test public void testOptionalSupportingCollector () {
        Mustache.Compiler comp = Mustache.compiler().withCollector(new DefaultCollector() {
            public Iterator<?> toIterator (final Object value) {
                if (value instanceof Optional<?>) {
                    Optional<?> opt = (Optional<?>) value;
                    return opt.isPresent() ? Collections.singleton(opt.get()).iterator() :
                        Collections.emptyList().iterator();
                } else return super.toIterator(value);
            }
        });
        test(comp, "test", "{{#foo}}{{.}}{{/foo}}", context("foo", Optional.of("test")));
        test(comp, "", "{{#foo}}{{.}}{{/foo}}", context("foo", Optional.empty()));
        test(comp, "", "{{^foo}}{{.}}{{/foo}}", context("foo", Optional.of("test")));
        test(comp, "test", "{{^foo}}test{{/foo}}", context("foo", Optional.empty()));
    }

    @Test public void testCompoundVariable () {
        test("hello", "{{foo.bar.baz}}", new Object() {
            Object foo () {
                return new Object() {
                    Object bar = new Object() {
                        String baz = "hello";
                    };
                };
            }
        });
    }

    @Test public void testNullComponentInCompoundVariable () {
        try {
            test(Mustache.compiler(), "unused", "{{foo.bar.baz}}", new Object() {
                Object foo = new Object() {
                    Object bar = null;
                };
            });
            fail();
        } catch (MustacheException me) {} // expected
    }

    @Test public void testMissingComponentInCompoundVariable () {
        try {
            test(Mustache.compiler(), "unused", "{{foo.bar.baz}}", new Object() {
                Object foo = new Object(); // no bar
            });
            fail();
        } catch (MustacheException me) {} // expected
    }

    @Test public void testNullComponentInCompoundVariableWithDefault () {
        test(Mustache.compiler().nullValue("null"), "null", "{{foo.bar.baz}}", new Object() {
            Object foo = null;
        });
        test(Mustache.compiler().nullValue("null"), "null", "{{foo.bar.baz}}", new Object() {
            Object foo = new Object() {
                Object bar = null;
            };
        });
    }

    @Test public void testMissingComponentInCompoundVariableWithDefault () {
        test(Mustache.compiler().defaultValue("?"), "?", "{{foo.bar.baz}}", new Object() {
            // no foo, no bar
        });
        test(Mustache.compiler().defaultValue("?"), "?", "{{foo.bar.baz}}", new Object() {
            Object foo = new Object(); // no bar
        });
    }

    @Test public void testCompoundVariableAsPlain () {
        // if a compound variable is found without decomposition, we use that first
        test("wholekey", "{{foo.bar}}", context(
                 "foo.bar", "wholekey",
                 "foo", new Object() { String bar = "hello"; }));
    }

    @Test public void testShadowedContextWithNull () {
        Mustache.Compiler comp = Mustache.compiler().nullValue("(null)");
        String tmpl = "{{foo}}{{#inner}}{{foo}}{{/inner}}", expect = "outer(null)";
        test(comp, expect, tmpl, new Object() {
            public String foo = "outer";
            public Object inner = new Object() {
                // this foo should shadow the outer foo even though it's null
                public String foo = null;
            };
        });
        // same as above, but with maps instead of objects
        test(comp, expect, tmpl, context("foo", "outer", "inner", context("foo", null)));
    }

    @Test public void testContextPokingLambda () {
        Mustache.Compiler c = Mustache.compiler();

        class Foo { public int foo = 1; }
        final Template lfoo = c.compile("{{foo}}");
        check("1", lfoo.execute(new Foo()));

        class Bar { public String bar = "one"; }
        final Template lbar = c.compile("{{bar}}");
        check("one", lbar.execute(new Bar()));

        test(c, "1oneone1one!", "{{#events}}{{#renderEvent}}{{/renderEvent}}{{/events}}", context(
                 "renderEvent", new Mustache.Lambda() {
                     public void execute (Template.Fragment frag, Writer out) throws IOException {
                         Object ctx = frag.context();
                         if (ctx instanceof Foo) lfoo.execute(ctx, out);
                         else if (ctx instanceof Bar) lbar.execute(ctx, out);
                         else out.write("!");
                     }
                 },
                 "events", Arrays.asList(
                     new Foo(), new Bar(), new Bar(), new Foo(), new Bar(), "wtf?")));
    }

    @Test public void testCustomFormatter () {
        Mustache.Formatter fmt = new Mustache.Formatter() {
            public String format (Object value) {
                if (value instanceof Date) return _fmt.format((Date)value);
                else return String.valueOf(value);
            }
            protected SimpleDateFormat _fmt = new SimpleDateFormat("yyyy/MM/dd"); {
                _fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
        };
        check("Date: 2014/01/08", Mustache.compiler().withFormatter(fmt).
              compile("{{msg}}: {{today}}").execute(new Object() {
                  String msg = "Date";
                  Date today = new Date(1389208567874L);
              }));
    }

    @Test public void testMapEntriesPlusReflectSection () {
        Map<String,String> data = new HashMap<String,String>();
        data.put("k1", "v1");
        data.put("k2", "v2");
        // 'key' and 'value' here rely on reflection so we can't test this in GWT
        test("k1v1k2v2", "{{#map.entrySet}}{{key}}{{value}}{{/map.entrySet}}",
             context("map", data));
    }

    //My test -
    @Test public void testMethodArg () {
        Object obj = new Object() {
            private String foo = "foo value";

            public String getFoo() {
                return foo;
            }

            private BigDecimal bigDecimal = new BigDecimal(2.5);

            public Long aLong = 123L;

            private Date date = new Date(1527131159000L);//2018-05-24T03:05:59+00:00

            private LocalDateTime localDateTime = LocalDateTime.of(2018, 5, 24,11,8,20);

            public ZonedDateTime zonedDateTime = ZonedDateTime.of(2018, 5, 24, 11, 8, 20, 50, ZoneId.systemDefault());

            public BigDecimal getBigDecimal() {
                return bigDecimal;
            }

            public Date getDate() {
                return date;
            }

            public LocalDateTime getLocalDateTime() {
                return localDateTime;
            }
        };
        test("foo value__", "{{foo,l=11&t=_&a=0}}", obj);
        test("___foo value", "{{foo,l=12&t=_&a=1}}", obj);
        test("___foo value___", "{{foo,l=15&t=_&a=2}}", obj);
        test("__foo value___", "{{foo,l=14&t=_&a=2}}", obj);

        test("foo value  ", "{{foo,l=11&a=0}}", obj);
        test("   foo value", "{{foo,l=12&a=1}}", obj);
        test("   foo value   ", "{{foo,l=15&a=2}}", obj);
        test("  foo value   ", "{{foo,l=14&a=2}}", obj);

        //not default & separator ;
        test("foo value__", "{{foo,;,l=11;t=_;a=0}}", obj);
        test("__foo value___", "{{foo,;,l=14;t=_;a=2}}", obj);

        test("______2.50", "{{bigDecimal,l=10&t=_&a=1&f=0.00}}", obj);
        test("____123.00", "{{aLong,l=10&t=_&a=1&f=0.00}}", obj);
        test("_20180524110559_", "{{date,l=16&t=_&a=2&f=yyyyMMddHHmmss}}", obj);
        test("_20180524110820_", "{{localDateTime,l=16&t=_&a=2&f=yyyyMMddHHmmss}}", obj);
        test("_20180524110820_", "{{zonedDateTime,l=16&t=_&a=2&f=yyyyMMddHHmmss}}", obj);

    }
}
