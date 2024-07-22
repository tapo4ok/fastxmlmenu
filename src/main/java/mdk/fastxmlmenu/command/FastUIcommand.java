package mdk.fastxmlmenu.command;

import com.google.common.collect.ImmutableList;
import mdk.fastxmlmenu.FastXMLmenu;
import mdk.fastxmlmenu.ui.UI;
import mdk.fastxmlmenu.xml.Loader;
import mdk.mutils.Identifier;
import org.apache.commons.lang.Validate;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class FastUIcommand implements CommandExecutor, TabExecutor {
    private final String[] w1 = new String[] {
            "reload",
            "open",
            "help"
    };
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(args.length>0)) {
            sender.sendMessage(String.format("/%s (%s)", "fastui", String.join("/", w1)));
            return false;
        }

        switch (args[0]) {
            case "open": {
                Identifier identifier = new Identifier(args[1], args[2]);
                for (UI ui : UI.REGISTRY) {
                    if (ui.getIdentifier().equals(identifier)) {
                        ui.open((Player) sender);
                    }
                }
                break;
            }
            case "reload": {
                JavaPlugin plugin = JavaPlugin.getPlugin(FastXMLmenu.class);

                UI.us.clear();
                UI.u.clear();

                Loader loader = Loader.getInstance();
                File data = plugin.getDataFolder();
                if (!data.exists()) data.mkdirs();

                File[] uis = data.listFiles(FastXMLmenu.filter);

                assert uis != null;
                for (File file : uis) {
                    try {
                        UI ui = loader.load(Files.newInputStream(file.toPath()));
                        ui.regster(plugin);
                        plugin.getLogger().log(Level.INFO, String.format("Load ui %s", ui.getIdentifier()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case "help":
            default:
            {
                sender.sendMessage("/fastui reload");
                sender.sendMessage("/fastui help");
                sender.sendMessage("/fastui open <namespace> <path>");
                break;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        if (args.length == 0) {
            return ImmutableList.of();
        } else if (args.length == 1) {
            String lastWord = args[args.length - 1];
            List<String> matchedPlayers = new ArrayList();
            Iterator<String> var7 = Arrays.stream(w1).iterator();
            while(var7.hasNext()) {
                String name = (String)var7.next();
                if (StringUtil.startsWithIgnoreCase(name, lastWord)) {
                    matchedPlayers.add(name);
                }
            }

            Collections.sort(matchedPlayers, String.CASE_INSENSITIVE_ORDER);
            return matchedPlayers;
        }

        return ImmutableList.of();
    }
}