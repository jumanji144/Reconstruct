package me.darknet.reconstruct.model.phantom;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;

import java.util.Set;

public interface PhantomUnit {

    /**
     * Add a hint that this class is somewhere related to the parent class
     * @param parent the parent unit
     */
    void addTypeHint(PhantomUnit parent);

    /**
     * Get the type of this class
     * @return the type of this class
     */
    InstanceType type();

    /**
     * Get the parent candidates of this class
     * @return the parent candidates of this class
     */
    Set<PhantomUnit> parentCandidates();

    /**
     * Get the children candidates of this class
     * @return the children candidates of this class
     */
    Set<PhantomUnit> childrenCandidates();

    /**
     * Add a method to this class
     * @param access the access of the method
     * @param name the name of the method
     * @param type the type of the method
     */
    void putMethod(int access, String name, MethodType type);

    /**
     * Add a field to this class
     * @param access the access of the field
     * @param name the name of the field
     * @param type the type of the field
     */
    void putField(int access, String name, ClassType type);

    /**
     * Determines if this is a concrete unit, meaning it has been fully defined
     * @return if this is a concrete unit
     */
    boolean concrete();

    /**
     * Get the access of this class
     * @return the access of this class
     */
    int access();

    /**
     * Set the access of this class
     * @param access the access of this class
     */
    void access(int access);
}
