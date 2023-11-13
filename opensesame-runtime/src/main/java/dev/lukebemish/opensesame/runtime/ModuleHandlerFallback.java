package dev.lukebemish.opensesame.runtime;

class ModuleHandlerFallback implements ModuleHandler {
    @Override
    public boolean openModule(Module to, Module target, String className) {
        if (to == target) return true;
        String packageName = className.substring(0, className.lastIndexOf('.'));

        if (target.getPackages().contains(packageName)) {
            if (!target.isOpen(packageName, to)) {
                target.addOpens(packageName, to);
            }
            return true;
        }
        return false;
    }
}
