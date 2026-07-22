import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import java.io.File;

public class AttachTool {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("=== Uzycie AttachTool ===");
            System.out.println("Sposob uzycia: java AttachTool.java <PID> <sciezka_do_agenta_jar>");
            System.out.println("\nLista aktualnie uruchomionych procesow Java (JVM):");
            for (VirtualMachineDescriptor desc : VirtualMachine.list()) {
                System.out.printf("  PID: %-6s | %s\n", desc.id(), desc.displayName());
            }
            return;
        }

        String pid = args[0];
        String agentPath = new File(args[1]).getAbsolutePath();

        try {
            System.out.println("Laczenie z procesem o PID: " + pid);
            VirtualMachine vm = VirtualMachine.attach(pid);
            
            System.out.println("Wstrzykiwanie agenta: " + agentPath);
            vm.loadAgent(agentPath);
            
            vm.detach();
            System.out.println("Sukces! Agent zostal prawidlowo wstrzykniety i odpalony.");
        } catch (Exception e) {
            System.err.println("Blad podczas wstrzykiwania agenta:");
            e.printStackTrace();
        }
    }
}
