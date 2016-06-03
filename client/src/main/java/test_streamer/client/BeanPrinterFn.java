package test_streamer.client;

import us.bpsm.edn.Keyword;
import us.bpsm.edn.printer.Printer;
import us.bpsm.edn.printer.Printers;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;


/**
 * @author kawasima
 */
public class BeanPrinterFn implements Printer.Fn {
    @Override
    public void eval(Object bean, Printer printer) {
        Printer.Fn<Object> fn = (Printer.Fn<Object>)Printers.defaultPrinterProtocol().lookup(bean.getClass());
        if (fn != null) {
            fn.eval(bean, printer);
        } else {
            try {
                BeanInfo info = Introspector.getBeanInfo(bean.getClass());
                Map<Keyword, Object> beanMap = new HashMap<Keyword, Object>();
                for (PropertyDescriptor propDesc : info.getPropertyDescriptors()) {
                    if (propDesc.getName().equals("class"))
                        continue;
                    try {
                        Object val = propDesc.getReadMethod().invoke(bean);

                        PropertyName propAnno = propDesc.getReadMethod().getAnnotation(PropertyName.class);
                        String propName = (propAnno != null) ? propAnno.value() : propDesc.getName();
                        beanMap.put(keywordize(propName), val);
                    } catch(Exception ignore) {}
                }
                printer.printValue(beanMap);
            } catch (IntrospectionException ex) {
                throw new IllegalArgumentException("Not bean.");
            }
        }
    }

    protected Keyword keywordize(String name) {
        if (name == null)
            return null;

        if (name.length() == 1)
            return Keyword.newKeyword(name);

        StringBuilder sb = new StringBuilder(40);
        int pos = 0;
        for (int i=1; i<name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i))) {
                if (sb.length() != 0) {
                    sb.append('-');
                }
                sb.append(name.substring(pos, i).toLowerCase());
                pos = i;
            }
        }
        if (sb.length() != 0)
            sb.append('-');
        sb.append(name.substring(pos, name.length()).toLowerCase());
        return Keyword.newKeyword(sb.toString());
    }
}
