package mdk.fastxmlmenu.menu;

import mdk.fastxmlmenu.MENUEvent;
import mdk.fastxmlmenu.MENUMethod;
import mdk.fastxmlmenu.command.MenuCommand;
import mdk.fastxmlmenu.fun.Function;
import mdk.fastxmlmenu.hadler.IHandler;
import mdk.fastxmlmenu.xml.Placeholder;
import mdk.mutils.Identifier;
import mdk.mutils.registry.Registry;
import mdk.mutils.registry.SimpleRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;

public class Menu {
    public static final Map<Identifier, Menu> u = new HashMap<>();
    public static final List<Menu> us = new ArrayList<>();
    public static final Registry<Menu> REGISTRY = new SimpleRegistry<Menu>(new Identifier("fastmenuxml", "menu_registry"), u,  us);


    private Map<String, Function> functionMap;
    private final Identifier identifier;
    private IHandler handler;
    private final List<Entry> entries;
    private final String title;
    private Optional<MenuCommand> command;

    public Menu(Identifier identifier, IHandler handler, String title) {
        this.identifier = identifier;
        this.handler = handler;
        this.entries = new ArrayList<>();
        this.title = title;
        this.functionMap = new HashMap<>();
        this.command = Optional.empty();
    }

    public void setFunctions(Map<String, Function> functionMap) {
        this.functionMap = functionMap;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public IHandler getHandler() {
        return handler;
    }

    public void setCommand(Optional<MenuCommand> command) {
        this.command = command;
    }

    public void add(ItemStack stack, String method, int slot) {
        entries.add(new Entry(method, slot, stack));
    }

    public void setHandler(IHandler handler) {
        this.handler = handler;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        for (Entry entry : entries) {
            inventory.setItem(entry.slot, entry.stack);
        }

        player.openInventory(inventory);
    }

    public void regster(JavaPlugin plugin) {
        Registry.register(REGISTRY, identifier, this);
        command.ifPresent(uiCommand -> uiCommand.register(plugin));
    }

    public static class Entry {
        public String method;
        public int slot;
        public ItemStack stack;

        public Entry(String method, int slot, ItemStack stack) {
            this.method = method;
            this.slot = slot;
            this.stack = stack;
        }
    }

    public static class EventHandler implements Listener {
        @org.bukkit.event.EventHandler
        public void a(InventoryClickEvent event) {
            for (Menu ui : REGISTRY) {
                if (ui.title.equalsIgnoreCase(event.getView().getTitle())) {
                    for (Entry entry : ui.entries) {
                        if (entry.slot == event.getSlot()) {
                            if (ui.functionMap.containsKey(entry.method)) {
                                Function function = ui.functionMap.get(entry.method);
                                HumanEntity entity = event.getWhoClicked();

                                Placeholder holder = Placeholder.getInstance();
                                Map<String, String> map = new HashMap<>();
                                map.put("player", event.getWhoClicked().getName());

                                switch (function.getSender()) {
                                    case SERVER:
                                        for (String line : function.getLines()) {
                                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), holder.replacePlaceholders(line, map));
                                        }
                                        break;
                                    case CLIENT:
                                        for (String line : function.getLines()) {
                                            Bukkit.dispatchCommand(entity, holder.replacePlaceholders(line, map));
                                        }
                                    case CLIENT_OP:
                                        boolean op = entity.isOp();
                                        for (String line : function.getLines()) {
                                            entity.setOp(true);
                                            Bukkit.dispatchCommand(entity, holder.replacePlaceholders(line, map));
                                            entity.setOp(false);
                                        }
                                        entity.setOp(op);
                                }

                                if (function.isCancelEvent()) {
                                    event.setCancelled(true);
                                }
                                break;
                            }



                            Class<?> cls = ui.handler.getClass();
                            try {
                                Method method = cls.getMethod(entry.method, Menu.class, InventoryClickEvent.class);
                                MENUMethod method1 = method.getAnnotation(MENUMethod.class);
                                if (method1 == null) {
                                    break;
                                }


                                method.setAccessible(true);

                                try {
                                    method.invoke(ui.handler, ui, event);
                                } catch (Exception e) {
                                    for (Method method2 : cls.getMethods()) {
                                        MENUEvent event1 = method2.getAnnotation(MENUEvent.class);
                                        if (event1 != null) {
                                            if (event1.value().equalsIgnoreCase("catch")) {
                                                method2.invoke(ui.handler, ui, event, e);
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (method1.auto_canceable()) {
                                    event.setCancelled(true);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }

                    if (event.getAction() == InventoryAction.PLACE_ALL ||
                            event.getAction() == InventoryAction.PLACE_ONE ||
                            event.getAction() == InventoryAction.PLACE_SOME) {
                        event.setCancelled(true);
                    }
                }
            }
        }

        @org.bukkit.event.EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            for (Menu ui : REGISTRY) {
                if (ui.title.equalsIgnoreCase(event.getView().getTitle())) {
                    Class<?> cls = ui.handler.getClass();
                    try {
                        for (Method method : cls.getMethods()) {
                            MENUEvent event1 = method.getAnnotation(MENUEvent.class);
                            if (event1 != null) {
                                if (event1.value().equalsIgnoreCase("close")) {
                                    method.invoke(ui.handler, ui, event);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }

        @org.bukkit.event.EventHandler
        public void onInventoryOpen(InventoryOpenEvent event) {
            for (Menu ui : REGISTRY) {
                if (ui.title.equalsIgnoreCase(event.getView().getTitle())) {
                    Class<?> cls = ui.handler.getClass();
                    try {
                        for (Method method : cls.getMethods()) {
                            MENUEvent event1 = method.getAnnotation(MENUEvent.class);
                            if (event1 != null) {
                                if (event1.value().equalsIgnoreCase("open")) {
                                    method.invoke(ui.handler, ui, event);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }
}
