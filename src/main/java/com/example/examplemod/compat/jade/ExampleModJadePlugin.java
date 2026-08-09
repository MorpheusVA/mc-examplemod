package com.example.examplemod.compat.jade;

import com.example.examplemod.content.DynamicBlock;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ExampleModJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Register custom block tooltip provider for all DynamicBlock instances
        registration.registerBlockComponent(ExampleBlockComponentProvider.INSTANCE, DynamicBlock.class);
    }
}
