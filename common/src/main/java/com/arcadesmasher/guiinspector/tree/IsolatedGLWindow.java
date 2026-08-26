package com.arcadesmasher.guiinspector.tree;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.internal.ImGuiContext;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

// geez GL contexts are annoying.
// a lot of stuff here is very fragile and deliberate, so probably just don't touch this class at all
public final class IsolatedGLWindow {

	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread renderThread;

	public static final Object POLL_LOCK = new Object();

	private ImGuiContext imGuiContext;
	private ImGuiImplGlfw imGuiGlfw;
	private ImGuiImplGl3 imGuiGl3;

	private volatile long windowHandle = MemoryUtil.NULL;

	private volatile Runnable contentRenderer;
	private final ConcurrentLinkedQueue<Runnable> contextTasks = new ConcurrentLinkedQueue<>();

	public void setContentRenderer(Runnable contentRenderer) {
		this.contentRenderer = contentRenderer;
	}

	// make sure GL exists before calling this
	public void start() {
		if (running.compareAndSet(false, true)) {
			renderThread = new Thread(this::runLoop, "GUIInspector-GL-Thread");
			renderThread.setDaemon(false);
			renderThread.start();
		}
	}

	public void stop() {
		running.set(false);
		if (renderThread != null) {
			try {
				renderThread.join(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void runLoop() {
		long window = MemoryUtil.NULL;
		try {
			GLFW.glfwDefaultWindowHints();
			GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
			GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
			GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
			GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
			GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
			GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

			window = GLFW.glfwCreateWindow(
					800, 600,
					"GUI Inspector",
					MemoryUtil.NULL,
					MemoryUtil.NULL
			);

			if (window == MemoryUtil.NULL) {
				throw new IllegalStateException("Failed to create GLFW window");
			}
			windowHandle = window;

			GLFW.glfwSetWindowCloseCallback(window, w -> {
				GLFW.glfwSetWindowShouldClose(w, false);
				GLFW.glfwHideWindow(w);
			});

			GLFW.glfwMakeContextCurrent(window);
			GLFW.glfwSwapInterval(1);
			GLCapabilities caps = GL.createCapabilities();

			imGuiContext = ImGui.createContext();
			ImGui.setCurrentContext(imGuiContext);

			final ImGuiIO io = ImGui.getIO();
			io.setIniFilename(null);

			imGuiGlfw = new ImGuiImplGlfw();
			imGuiGlfw.init(window, true);

			imGuiGl3 = new ImGuiImplGl3();
			imGuiGl3.init("#version 330 core");

			while (running.get() && !GLFW.glfwWindowShouldClose(window)) {
				GL.setCapabilities(caps);
				ImGui.setCurrentContext(imGuiContext);

				Runnable task;
				while ((task = contextTasks.poll()) != null) {
					try {
						task.run();
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}

				render();
				renderImGui();

				GLFW.glfwSwapBuffers(window);

				synchronized (POLL_LOCK) {
					GLFW.glfwPollEvents();
				}
			}
		} finally {
			// dispose ImGui stuff on THIS thread, before the GL context or window go away
			if (imGuiGl3 != null) {
				imGuiGl3.shutdown();
			}
			if (imGuiGlfw != null) {
				imGuiGlfw.shutdown();
			}
			if (imGuiContext != null) {
				ImGui.destroyContext(imGuiContext);
			}

			if (window != MemoryUtil.NULL) {
				Callbacks.glfwFreeCallbacks(window);
				GLFW.glfwDestroyWindow(window);
			}
			windowHandle = MemoryUtil.NULL;
			// deliberately not calling GL.setCapabilities(null) globally
			running.set(false);
		}
	}

	public void show() {
		long w = windowHandle;
		if (w != MemoryUtil.NULL) {
			GLFW.glfwShowWindow(w);
		}
	}

	public void hide() {
		long w = windowHandle;
		if (w != MemoryUtil.NULL) {
			GLFW.glfwHideWindow(w);
		}
	}

	public void setTitle(String title) {
		long w = windowHandle;
		if (w != MemoryUtil.NULL) {
			GLFW.glfwSetWindowTitle(w, title);
		}
	}

	public void toggleVisible() {
		long w = windowHandle;
		if (w != MemoryUtil.NULL) {
			if (GLFW.glfwGetWindowAttrib(w, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE) {
				GLFW.glfwHideWindow(w);
			} else {
				GLFW.glfwShowWindow(w);
			}
		}
	}

	public boolean isVisible() {
		long w = windowHandle;
		return w != MemoryUtil.NULL
				&& GLFW.glfwGetWindowAttrib(w, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE;
	}

	public <T> T callInContext(Callable<T> task) throws Exception {
		if (Thread.currentThread() == renderThread) {
			return task.call();
		}

		FutureTask<T> future = new FutureTask<>(task);
		contextTasks.add(future);

		return future.get();
	}

	private void render() {
		org.lwjgl.opengl.GL11.glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
		org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);
	}

	private void renderImGui() {
		imGuiGl3.newFrame();
		imGuiGlfw.newFrame();
		ImGui.newFrame();

		if (contentRenderer != null) {
			contentRenderer.run();
		}

		ImGui.render();
		imGuiGl3.renderDrawData(ImGui.getDrawData());
	}
}