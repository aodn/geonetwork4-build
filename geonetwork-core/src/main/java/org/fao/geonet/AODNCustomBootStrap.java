package org.fao.geonet;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 *  This is a hack to use the same namespace as geonetwork, so that the initial scan pick this up, without the need
 *  to create maven project and custom xml.
 *
 *  You should not add any code to this class, the only purpose of this class is to expand the component scan
 *  package without using xml file.
 */
@Configuration
@ComponentScan({"au.org.aodn.geonetwork4"})
public class AODNCustomBootStrap {
}
