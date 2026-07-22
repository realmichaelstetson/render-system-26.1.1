package com.haloclient.client.render.model.obj;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ObjModelLoader {

    public static ObjModel loadModel(InputStream inputStream) {
        ObjModel model = new ObjModel();

        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> colors = new ArrayList<>();
        List<ObjModel.Face> loadedFaces = new ArrayList<>();
        Map<ObjModel.Face, List<Integer>> facePosIndices = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int currentColor = -1;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                String token = parts[0].toLowerCase();

                switch (token) {
                    case "v": // Vertex position (x, y, z, [r, g, b])
                        float x = Float.parseFloat(parts[1]);
                        float y = Float.parseFloat(parts[2]);
                        float z = Float.parseFloat(parts[3]);
                        positions.add(new float[]{x, y, z});

                        if (parts.length >= 7) {
                            int r = parseColorComponent(parts[4]);
                            int g = parseColorComponent(parts[5]);
                            int b = parseColorComponent(parts[6]);
                            colors.add(new int[]{r, g, b});
                        } else {
                            colors.add(new int[]{255, 255, 255});
                        }
                        break;

                    case "vt": // Texture coordinates (u, v)
                        float u = Float.parseFloat(parts[1]);
                        float v = parts.length > 2 ? Float.parseFloat(parts[2]) : 0.0f;
                        uvs.add(new float[]{u, v});
                        break;

                    case "vn": // Normal (nx, ny, nz)
                        float nx = Float.parseFloat(parts[1]);
                        float ny = Float.parseFloat(parts[2]);
                        float nz = Float.parseFloat(parts[3]);
                        normals.add(new float[]{nx, ny, nz});
                        break;

                    case "usemtl": // Material color directive
                        if (parts.length > 1 && parts[1].startsWith("#") && parts[1].length() == 7) {
                            currentColor = Integer.parseInt(parts[1].substring(1), 16);
                        }
                        break;

                    case "f": // Face
                        ObjModel.Face face = new ObjModel.Face();
                        face.colorOverride = currentColor;
                        List<Integer> posIndicesList = new ArrayList<>();

                        for (int i = 1; i < parts.length; i++) {
                            String[] indices = parts[i].split("/");
                            int posIdx = parseIndex(indices[0], positions.size());
                            int uvIdx = (indices.length > 1 && !indices[1].isEmpty()) ? parseIndex(indices[1], uvs.size()) : -1;
                            int normIdx = (indices.length > 2 && !indices[2].isEmpty()) ? parseIndex(indices[2], normals.size()) : -1;

                            float[] pos = positions.get(posIdx);
                            float[] uv = uvIdx >= 0 ? uvs.get(uvIdx) : new float[]{0.0f, 0.0f};
                            float[] norm = normIdx >= 0 ? normals.get(normIdx) : new float[]{0.0f, 1.0f, 0.0f};
                            int[] col = colors.size() > posIdx ? colors.get(posIdx) : new int[]{255, 255, 255};

                            ObjModel.Vertex vertex = new ObjModel.Vertex(
                                pos[0], pos[1], pos[2],
                                uv[0], uv[1],
                                norm[0], norm[1], norm[2],
                                col[0], col[1], col[2]
                            );

                            face.vertices.add(vertex);
                            posIndicesList.add(posIdx);
                        }

                        loadedFaces.add(face);
                        facePosIndices.put(face, posIndicesList);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Build vertex-to-vertex adjacency graph from faces
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (ObjModel.Face face : loadedFaces) {
            List<Integer> posList = facePosIndices.get(face);
            if (posList == null) continue;
            for (int i = 0; i < posList.size(); i++) {
                int v1 = posList.get(i);
                for (int j = i + 1; j < posList.size(); j++) {
                    int v2 = posList.get(j);
                    adj.computeIfAbsent(v1, k -> new HashSet<>()).add(v2);
                    adj.computeIfAbsent(v2, k -> new HashSet<>()).add(v1);
                }
            }
        }

        // Traverse connected mesh graph via BFS to extract exact 3D components
        int[] vertexCompId = new int[positions.size()];
        Arrays.fill(vertexCompId, -1);
        int compCount = 0;

        for (int v = 0; v < positions.size(); v++) {
            if (vertexCompId[v] == -1) {
                Queue<Integer> queue = new ArrayDeque<>();
                vertexCompId[v] = compCount;
                queue.add(v);
                while (!queue.isEmpty()) {
                    int curr = queue.poll();
                    Set<Integer> neighbors = adj.get(curr);
                    if (neighbors != null) {
                        for (int neighbor : neighbors) {
                            if (vertexCompId[neighbor] == -1) {
                                vertexCompId[neighbor] = compCount;
                                queue.add(neighbor);
                            }
                        }
                    }
                }
                compCount++;
            }
        }

        // Exact 3D connected mesh component IDs for legs and arms:
        Set<Integer> leftLegComps = Set.of(1, 2, 3, 4, 5, 6, 20, 21, 39, 78, 79, 91, 92);
        Set<Integer> rightLegComps = Set.of(44, 45, 46, 47, 48, 51, 52, 53, 87, 89, 115, 116, 117, 118);
        Set<Integer> leftArmComps = Set.of(0, 8, 9, 10, 75, 76, 77, 102, 106, 107, 108, 111, 112); // Left Arm & Outer Baseball Bat
        Set<Integer> rightArmComps = Set.of(
            54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74,
            88, 90, 93, 94, 95, 96, 97, 100, 119, 120, 125
        );

        // Assign partId with 100% topological & geometry precision
        for (ObjModel.Face face : loadedFaces) {
            List<Integer> posList = facePosIndices.get(face);
            for (int i = 0; i < face.vertices.size(); i++) {
                ObjModel.Vertex vertex = face.vertices.get(i);
                int posIdx = posList != null && i < posList.size() ? posList.get(i) : -1;
                int compId = (posIdx >= 0 && posIdx < vertexCompId.length) ? vertexCompId[posIdx] : -1;

                if (leftLegComps.contains(compId)) {
                    vertex.partId = 0; // Left Leg
                } else if (rightLegComps.contains(compId)) {
                    vertex.partId = 1; // Right Leg
                } else if (leftArmComps.contains(compId)
                        || ((compId == 25 || compId == 7 || compId == 12 || compId == 17 || compId == 22 || compId == 36 || compId == 35 || compId == 81) && vertex.x < -1.0f)
                        || (vertex.x < -1.0f && vertex.y > 5.0f && vertex.y < 15.0f)
                        || (vertex.x < -2.5f && vertex.y >= 15.0f && vertex.y < 23.0f)) {
                    vertex.partId = 2; // Left Arm & Bat: everything at X < -1.0 in arm zone moves with arm
                } else if (rightArmComps.contains(compId) || (compId == 50 && vertex.x >= 5.0f)) {
                    vertex.partId = 3; // Right Arm (Comp 50 threshold at X=5.0 - trunk never reaches X=5)
                } else {
                    vertex.partId = 4; // Log Body / Head / Face
                }
            }
            model.addFace(face);
        }

        // DEBUG: Print partition statistics
        int[] partCounts = new int[5];
        int batArm = 0, batBody = 0, batLeg = 0;
        for (ObjModel.Face face : loadedFaces) {
            for (ObjModel.Vertex v : face.vertices) {
                partCounts[v.partId]++;
                // Bat area: X < -3, Y > 15
                if (v.x < -3.0f && v.y > 15.0f) {
                    if (v.partId == 2) batArm++;
                    else if (v.partId == 0) batLeg++;
                    else batBody++;
                }
            }
        }
        System.out.println("[ObjModelLoader] PartId stats: LeftLeg=" + partCounts[0] 
            + " RightLeg=" + partCounts[1] + " LeftArm=" + partCounts[2] 
            + " RightArm=" + partCounts[3] + " Body=" + partCounts[4]);
        System.out.println("[ObjModelLoader] Bat area (X<-3,Y>15): Arm=" + batArm 
            + " Leg=" + batLeg + " Body=" + batBody);

        return model;
    }

    private static int parseIndex(String token, int listSize) {
        int idx = Integer.parseInt(token);
        if (idx > 0) {
            return idx - 1; // 1-indexed OBJ offset
        } else {
            return listSize + idx; // Negative index relative offset
        }
    }

    private static int parseColorComponent(String val) {
        float f = Float.parseFloat(val);
        if (f <= 1.0f) {
            return (int) (f * 255.0f);
        }
        return (int) f;
    }
}
